package cn.li.acapi.entity;

/**
 * Loader-neutral contract for an entity that represents one part of another entity.
 *
 * <p>The API module intentionally does not depend on Minecraft classes. Runtime
 * integrations validate that both the part and returned parent are entities before
 * using the relationship.</p>
 */
public interface MultipartEntityPart {

    /**
     * @return the immediate parent entity, or {@code null} when it is unavailable
     */
    Object getMultipartParent();
}
