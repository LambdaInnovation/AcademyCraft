package cn.academy.ability.context;

/**
 * A {@link Context} that provides this interface is used in CPBar rendering to render consumption hint.
 */
public interface IConsumptionProvider {

    /**
     * @return The consumption expected (called every frame)
     */
    float getConsumptionHint();

}