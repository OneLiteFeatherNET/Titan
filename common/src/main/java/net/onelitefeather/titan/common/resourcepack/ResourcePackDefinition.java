/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.resourcepack;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * One resource pack as it is configured: a stable identifier, the URL it is served from and
 * the SHA-1 of the archive behind that URL.
 *
 * <p>The identifier is what {@code resource_pack_pop} addresses, so it has to stay stable for
 * the base pack and has to differ per season for the season pack. The hash is mandatory: the
 * vanilla client caches downloaded packs <em>by URL, not by hash</em>, so a changed pack served
 * from an unchanged URL is never re-downloaded. Serve every pack under a content-addressed name
 * ({@code pack-<sha1>.zip}) and the cache problem disappears - {@link #contentAddressed()}
 * reports whether a configured URL follows that rule.
 *
 * @param id       the pack identifier sent to the client and used by {@code resource_pack_pop}
 * @param url      the URL the client downloads the archive from
 * @param hash     the SHA-1 of the archive, 40 hexadecimal characters
 * @param required whether the client must load the pack to stay connected
 * @param prompt   an optional MiniMessage prompt shown in the client's accept dialog
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record ResourcePackDefinition(UUID id, String url, String hash, boolean required,
                                     @Nullable String prompt) {

    private static final Pattern SHA1 = Pattern.compile("[0-9a-f]{40}");

    /**
     * Validates and normalises the configured values.
     *
     * @throws NullPointerException     if the identifier, URL or hash is missing
     * @throws IllegalArgumentException if the URL is not a valid URI or the hash is not a SHA-1
     */
    public ResourcePackDefinition {
        Objects.requireNonNull(id, "A resource pack needs an id");
        Objects.requireNonNull(url, "A resource pack needs an url");
        Objects.requireNonNull(hash, "A resource pack needs a sha1 hash");
        if (url.isBlank()) {
            throw new IllegalArgumentException("The resource pack url must not be blank");
        }
        try {
            new URI(url);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("The resource pack url is not a valid uri: " + url, exception);
        }
        hash = hash.trim().toLowerCase(Locale.ROOT);
        if (!SHA1.matcher(hash).matches()) {
            throw new IllegalArgumentException("The resource pack hash must be a 40 character sha1, got: " + hash);
        }
    }

    /**
     * The URL as a {@link URI}.
     *
     * @return the download location of the pack
     */
    public URI uri() {
        return URI.create(this.url);
    }

    /**
     * Whether the download URL carries the hash, so that a changed pack is served under a
     * changed URL. Only a content-addressed URL survives the client's URL-keyed cache.
     *
     * @return {@code true} if the URL contains the configured hash
     */
    public boolean contentAddressed() {
        return this.url.toLowerCase(Locale.ROOT).contains(this.hash);
    }

    /**
     * The Adventure description of this pack.
     *
     * @return the pack info carrying id, uri and hash
     */
    public ResourcePackInfo toPackInfo() {
        return ResourcePackInfo.resourcePackInfo(this.id, this.uri(), this.hash);
    }

    /**
     * Builds the push request for this single pack.
     *
     * <p>The request never sets {@code replace}: Minestom implements that flag as
     * pop-all-then-push, which makes the client drop and reload every pack it holds. Packs are
     * always added one at a time and removed by identifier.
     *
     * @param required whether the client must load the pack; overrides {@link #required()} so a
     *                 caller can soften the requirement per player
     * @return the request to hand to {@code Player#sendResourcePacks}
     */
    public ResourcePackRequest toRequest(boolean required) {
        ResourcePackRequest.Builder builder = ResourcePackRequest.resourcePackRequest().packs(this.toPackInfo()).replace(false).required(required);
        Component promptComponent = this.promptComponent();
        if (promptComponent != null) {
            builder.prompt(promptComponent);
        }
        return builder.build();
    }

    /**
     * The deserialised prompt shown in the client's accept dialog.
     *
     * @return the prompt, or {@code null} when none is configured
     */
    public @Nullable Component promptComponent() {
        if (this.prompt == null || this.prompt.isBlank()) {
            return null;
        }
        return MiniMessage.miniMessage().deserialize(this.prompt);
    }
}
