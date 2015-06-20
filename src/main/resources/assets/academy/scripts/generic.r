ac {
    #能力开发机
	developer {
		# tps: 每次刺激所消耗的时间 单位ticks
		basic {
			tps { 10 }
		}
		improved {
			tps { 8 }
		}
		advanced {
			tps { 5 }
		}
	}
	matrix {
	   capacity(N, L) { sqrt(N) * L * 6 } # 容量
	   latency(N, L) { N * L * L * 20 } # 带宽
	   range(N, L) { N * 8 * sqrt(L) } # 信号距离
	}
}