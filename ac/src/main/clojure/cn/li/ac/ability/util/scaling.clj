(ns cn.li.ac.ability.util.scaling
  "Experience-based scaling utilities for ability parameters.

  All scaling functions use linear interpolation (lerp) between min and max values
  based on skill experience (0.0-1.0). No Minecraft imports.")

(defn clamp-exp
  "Clamp skill experience to [0.0, 1.0]."
  [exp]
  (max 0.0 (min 1.0 (double (or exp 0.0)))))

(defn lerp
  "Linear interpolation between min-val and max-val based on t (0.0-1.0).

  Examples:
    (lerp 10.0 20.0 0.0)  => 10.0
    (lerp 10.0 20.0 0.5)  => 15.0
    (lerp 10.0 20.0 1.0)  => 20.0"
  [min-val max-val t]
  (+ min-val (* (- max-val min-val) (double t))))

(defn scale-damage
  "Scale damage based on skill experience.

  Args:
    base-min: minimum damage at 0% experience
    base-max: maximum damage at 100% experience
    exp: skill experience (0.0-1.0)

  Returns: scaled damage as double"
  [base-min base-max exp]
  (lerp (double base-min) (double base-max) (double exp)))

(defn scale-range
  "Scale range/distance based on skill experience.

  Args:
    range-min: minimum range at 0% experience
    range-max: maximum range at 100% experience
    exp: skill experience (0.0-1.0)

  Returns: scaled range as double"
  [range-min range-max exp]
  (lerp (double range-min) (double range-max) (double exp)))

(defn scale-cooldown
  "Scale cooldown based on skill experience (inverse - higher exp = lower cooldown).

  Args:
    cooldown-max: maximum cooldown at 0% experience (ticks)
    cooldown-min: minimum cooldown at 100% experience (ticks)
    exp: skill experience (0.0-1.0)

  Returns: scaled cooldown as int (ticks)"
  [cooldown-max cooldown-min exp]
  (int (lerp (double cooldown-max) (double cooldown-min) (double exp))))

(defn scale-cost
  "Scale energy cost based on skill experience (inverse - higher exp = lower cost).

  Args:
    cost-max: maximum cost at 0% experience
    cost-min: minimum cost at 100% experience
    exp: skill experience (0.0-1.0)

  Returns: scaled cost as double"
  [cost-max cost-min exp]
  (lerp (double cost-max) (double cost-min) (double exp)))

(defn scale-duration
  "Scale duration based on skill experience.

  Args:
    duration-min: minimum duration at 0% experience (ticks)
    duration-max: maximum duration at 100% experience (ticks)
    exp: skill experience (0.0-1.0)

  Returns: scaled duration as int (ticks)"
  [duration-min duration-max exp]
  (int (lerp (double duration-min) (double duration-max) (double exp))))
