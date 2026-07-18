/**
 * Workbench 图表 option 与进度聚合（配合全局 VChart / vue-echarts）
 */

export const WB_CHART = {
  accent: '#0891b2',
  deep: '#0e7490',
  light: '#22d3ee',
  track: 'rgba(8, 145, 178, 0.1)',
  grid: 'rgba(8, 145, 178, 0.08)',
  ink: '#0c2a42',
  body: '#3d6478',
  dim: '#8eb4c9',
  tooltipBg: 'rgba(255, 255, 255, 0.96)',
  status: {
    '0': '#7ba3b5',
    '1': '#0891b2',
    '2': '#0d9488',
    '3': '#c5d4de',
  },
  statusLabel: {
    '0': '待办',
    '1': '进行中',
    '2': '已完成',
    '3': '已取消',
  },
}

const tooltipBase = {
  backgroundColor: WB_CHART.tooltipBg,
  borderColor: 'rgba(8, 145, 178, 0.18)',
  borderWidth: 1,
  padding: [10, 12],
  textStyle: {
    color: WB_CHART.ink,
    fontSize: 12,
    fontFamily: "'Outfit', 'Noto Sans SC', sans-serif",
  },
  extraCssText: 'box-shadow: 0 8px 24px rgba(12, 42, 66, 0.1); border-radius: 10px;',
}

/** @param {Array} tasks */
export function countTaskStatus(tasks) {
  const counts = { '0': 0, '1': 0, '2': 0, '3': 0 }
  for (const t of tasks || []) {
    const s = String(t.status ?? '0')
    if (counts[s] != null) counts[s] += 1
    else counts['0'] += 1
  }
  return counts
}

/** completion = done / (total − cancelled) */
export function calcCompletion(tasks) {
  const list = tasks || []
  let done = 0
  let cancelled = 0
  for (const t of list) {
    const s = String(t.status ?? '0')
    if (s === '2') done += 1
    else if (s === '3') cancelled += 1
  }
  const eligible = list.length - cancelled
  const rate = eligible <= 0 ? 0 : Math.round((done / eligible) * 100)
  return { done, cancelled, eligible, total: list.length, rate }
}

export function statusDoughnutOption(counts, { title = '状态分布' } = {}) {
  const data = ['0', '1', '2', '3']
    .map((key) => ({
      name: WB_CHART.statusLabel[key],
      value: counts[key] || 0,
      itemStyle: { color: WB_CHART.status[key] },
    }))
    .filter((d) => d.value > 0)

  const empty = data.length === 0
  return {
    animationDuration: 650,
    animationEasing: 'cubicOut',
    tooltip: {
      ...tooltipBase,
      trigger: 'item',
      formatter: '{b}<br/>{c} · {d}%',
    },
    title: {
      text: title,
      left: 0,
      top: 0,
      textStyle: {
        color: WB_CHART.dim,
        fontSize: 11,
        fontWeight: 600,
        letterSpacing: 1,
        fontFamily: "'Outfit', 'Noto Sans SC', sans-serif",
      },
    },
    series: [
      {
        type: 'pie',
        radius: ['52%', '74%'],
        center: ['50%', '58%'],
        avoidLabelOverlap: true,
        padAngle: empty ? 0 : 2,
        itemStyle: {
          borderRadius: 4,
          borderColor: 'rgba(255,255,255,0.85)',
          borderWidth: 2,
        },
        label: { show: false },
        labelLine: { show: false },
        emphasis: {
          scale: true,
          scaleSize: 4,
          itemStyle: { shadowBlur: 12, shadowColor: 'rgba(8, 145, 178, 0.25)' },
        },
        data: empty
          ? [{ name: '暂无', value: 1, itemStyle: { color: WB_CHART.track }, tooltip: { show: false } }]
          : data,
      },
    ],
  }
}

export function completionGaugeOption(rate, { subtitle = '完成率' } = {}) {
  const pct = Math.max(0, Math.min(100, Number(rate) || 0))
  return {
    animationDuration: 700,
    animationEasing: 'cubicOut',
    series: [
      {
        type: 'gauge',
        startAngle: 210,
        endAngle: -30,
        min: 0,
        max: 100,
        radius: '88%',
        center: ['50%', '56%'],
        pointer: { show: false },
        anchor: { show: false },
        progress: {
          show: true,
          overlap: false,
          roundCap: true,
          clip: false,
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 1,
              y2: 1,
              colorStops: [
                { offset: 0, color: WB_CHART.light },
                { offset: 1, color: WB_CHART.deep },
              ],
            },
          },
        },
        axisLine: {
          lineStyle: {
            width: 12,
            color: [[1, WB_CHART.track]],
          },
        },
        splitLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false },
        detail: {
          valueAnimation: true,
          offsetCenter: [0, '4%'],
          formatter: '{value}%',
          color: WB_CHART.deep,
          fontSize: 28,
          fontWeight: 700,
          fontFamily: "'Outfit', 'Noto Sans SC', sans-serif",
        },
        title: {
          offsetCenter: [0, '32%'],
          color: WB_CHART.dim,
          fontSize: 12,
          fontWeight: 600,
          fontFamily: "'Noto Sans SC', sans-serif",
        },
        data: [{ value: pct, name: subtitle }],
      },
    ],
  }
}

/** @param {{ label: string, count: number }[]} marks */
export function dueBarOption(marks) {
  const labels = (marks || []).map((m) => m.label)
  const values = (marks || []).map((m) => m.count)
  return {
    animationDuration: 600,
    grid: { left: 8, right: 8, top: 36, bottom: 8, containLabel: true },
    tooltip: {
      ...tooltipBase,
      trigger: 'axis',
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(8, 145, 178, 0.06)' } },
    },
    title: {
      text: '近三日截止',
      left: 0,
      top: 0,
      textStyle: {
        color: WB_CHART.dim,
        fontSize: 11,
        fontWeight: 600,
        letterSpacing: 1,
        fontFamily: "'Outfit', 'Noto Sans SC', sans-serif",
      },
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: WB_CHART.grid } },
      axisLabel: {
        color: WB_CHART.body,
        fontSize: 12,
        fontWeight: 600,
        fontFamily: "'Outfit', 'Noto Sans SC', sans-serif",
      },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: WB_CHART.grid, type: 'dashed' } },
      axisLabel: {
        color: WB_CHART.dim,
        fontSize: 11,
        fontVariantNumeric: 'tabular-nums',
      },
    },
    series: [
      {
        type: 'bar',
        barWidth: 28,
        data: values.map((v, i) => ({
          value: v,
          itemStyle: {
            borderRadius: [8, 8, 4, 4],
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: i === 0 ? WB_CHART.light : WB_CHART.accent },
                { offset: 1, color: WB_CHART.deep },
              ],
            },
          },
        })),
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowColor: 'rgba(8, 145, 178, 0.3)' },
        },
      },
    ],
  }
}

/**
 * @param {Array} tasks
 * @param {import('dayjs').Dayjs} todayStart
 * @param {number} days
 */
export function completionTrendSeries(tasks, todayStart, days = 7) {
  const labels = []
  const keys = []
  for (let i = days - 1; i >= 0; i -= 1) {
    const d = todayStart.subtract(i, 'day')
    keys.push(d.format('YYYY-MM-DD'))
    labels.push(i === 0 ? '今' : d.format('M/D'))
  }
  const buckets = Object.fromEntries(keys.map((k) => [k, 0]))
  for (const t of tasks || []) {
    if (String(t.status) !== '2') continue
    const raw = t.updateTime || t.createTime
    if (!raw) continue
    const key = String(raw).slice(0, 10)
    if (buckets[key] != null) buckets[key] += 1
  }
  return { labels, values: keys.map((k) => buckets[k]) }
}

export function trendLineOption(labels, values, { title = '近 7 日完成' } = {}) {
  return {
    animationDuration: 700,
    grid: { left: 8, right: 12, top: 40, bottom: 8, containLabel: true },
    tooltip: {
      ...tooltipBase,
      trigger: 'axis',
    },
    title: {
      text: title,
      left: 0,
      top: 0,
      textStyle: {
        color: WB_CHART.dim,
        fontSize: 11,
        fontWeight: 600,
        letterSpacing: 1,
        fontFamily: "'Outfit', 'Noto Sans SC', sans-serif",
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: WB_CHART.grid } },
      axisLabel: {
        color: WB_CHART.body,
        fontSize: 11,
        fontWeight: 500,
        fontFamily: "'Outfit', 'Noto Sans SC', sans-serif",
      },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: WB_CHART.grid, type: 'dashed' } },
      axisLabel: {
        color: WB_CHART.dim,
        fontSize: 11,
        fontVariantNumeric: 'tabular-nums',
      },
    },
    series: [
      {
        type: 'line',
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 7,
        showSymbol: true,
        lineStyle: { width: 2.5, color: WB_CHART.accent },
        itemStyle: {
          color: '#fff',
          borderColor: WB_CHART.accent,
          borderWidth: 2,
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(34, 211, 238, 0.28)' },
              { offset: 1, color: 'rgba(8, 145, 178, 0.02)' },
            ],
          },
        },
        data: values,
      },
    ],
  }
}

/** Tiny doughnut for table cells */
export function miniProgressOption(rate) {
  const pct = Math.max(0, Math.min(100, Number(rate) || 0))
  const rest = 100 - pct
  return {
    animation: false,
    silent: true,
    series: [
      {
        type: 'pie',
        radius: ['68%', '92%'],
        center: ['50%', '50%'],
        silent: true,
        label: { show: false },
        labelLine: { show: false },
        data: [
          {
            value: pct || 0.001,
            itemStyle: {
              color: pct === 0 ? WB_CHART.track : WB_CHART.accent,
              borderRadius: 2,
            },
          },
          {
            value: rest || 0.001,
            itemStyle: { color: WB_CHART.track },
            tooltip: { show: false },
          },
        ],
      },
    ],
  }
}
