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
	matrix {
	   capacity(N, L) { sqrt(N) * L * 6 }
	   latency(N, L) { N * L * L * 20 }
	   range(N, L) { N * 8 * sqrt(L) }
	}
}