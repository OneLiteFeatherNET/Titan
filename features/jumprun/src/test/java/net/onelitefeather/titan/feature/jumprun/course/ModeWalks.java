package net.onelitefeather.titan.feature.jumprun.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.FakeSpaceProbe;
import net.onelitefeather.titan.feature.jumprun.space.Heading;

/**
 * Walks a generated course from its source block, shared by the PR-level and the sweep-level mode
 * checks. Each walk builds its own generator from its seed, so no walk depends on another.
 */
final class ModeWalks {

    static final CourseBlock SOURCE = TestBlocks.at(new BlockPos(0, 10, 0), Surface.FULL);
    static final Heading EAST = new Heading(1.0, 0.0);
    static final int JUMPS = 400;

    private ModeWalks() {
    }

    record Move(int score, Jump jump) {
    }

    static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    static List<Move> walk(Mode mode, long seed) {
        CourseGenerator generator = TestBlocks.generator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(seed));
        List<CourseBlock> course = new ArrayList<>(List.of(SOURCE));
        List<Move> moves = new ArrayList<>();
        Phase phase = new Phase.Scored(0, EAST, mode);
        for (int score = 0; score < JUMPS; score++) {
            Optional<CourseBlock> next = generator.next(course, phase);
            if (next.isEmpty()) {
                break;
            }
            moves.add(new Move(score, new Jump(course.getLast(), next.get())));
            course.add(next.get());
            phase = generator.after(course, phase);
        }
        return moves;
    }

    static boolean isTower(Jump jump) {
        return jump.to().climb().isPresent();
    }
}
