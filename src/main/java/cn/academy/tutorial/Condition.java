package cn.academy.tutorial;

import net.minecraft.entity.player.EntityPlayer;

import java.util.function.Predicate;

/**
 * A condition tests on a player and returns a boolean result. It is used to determine whether a tutorial is activated
 *  or not.
 * {@link Conditions} provides common types of conditions.
 */
public interface Condition extends Predicate<EntityPlayer> {

    default Condition or(Condition other) {
        return new OrCondition(this, other);
    }

    default Condition and(Condition other) {
        return new AndCondition(this, other);
    }

    class OrCondition implements Condition {
        private final Condition lhs;
        private final Condition rhs;

        public OrCondition(Condition lhs, Condition rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public boolean test(EntityPlayer entityPlayer) {
            return lhs.test(entityPlayer) || rhs.test(entityPlayer);
        }
    }

    class AndCondition implements Condition {
        private final Condition lhs;
        private final Condition rhs;

        public AndCondition(Condition lhs, Condition rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public boolean test(EntityPlayer entityPlayer) {
            return lhs.test(entityPlayer) && rhs.test(entityPlayer);
        }
    }

}
