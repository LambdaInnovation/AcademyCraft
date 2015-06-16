ac {
	developer {
		# ept: Energy consumed per tick for develop actions.
		basic {
			tps { 1 }
		}
		improved {
			tps { 0.8 }
		}
		advanced {
			tps { 0.5 }
		}
	}
	learning {
		learning_cost(skillLevel) { # In stimulations.
			3 + skillLevel * 1.5
		}
	}

}