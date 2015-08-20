ac {
    #能力开发机
	developer {
		# tps: 每次刺激所消耗的时间 单位ticks
		# cps: 每次刺激所消耗的IF
		portable {
			energy { 10000 }
			tps { 50 }
			cps { 750 }
		}
		normal {
			energy { 50000 }
			tps { 40 }
			cps { 700 }
		}
		advanced {
			energy { 200000 }
			tps { 30 }
			cps { 600 }
		}
	}
	matrix {
	   capacity(N, L) { sqrt(N) * L * 6 } # 容量
	   bandwidth(N, L) { N * L * L * 20 } # 带宽
	   range(N, L) { N * 8 * sqrt(L) } # 信号距离
	}
}