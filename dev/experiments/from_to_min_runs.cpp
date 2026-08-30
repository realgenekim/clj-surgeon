#include <algorithm>
#include <cstdint>
#include <limits>
#include <vector>

extern "C" int min_lcs_edit_runs(const unsigned char* a, int n,
                                  const unsigned char* b, int m,
                                  int* lcs_out) {
  const std::int64_t neg = std::numeric_limits<std::int64_t>::min() / 4;
  const std::int64_t base = static_cast<std::int64_t>(n) + m + 2;
  std::vector<std::int64_t> m_prev(m + 1, neg), e_prev(m + 1, neg);
  std::vector<std::int64_t> m_cur(m + 1, neg), e_cur(m + 1, neg);
  m_prev[0] = 0;
  for (int j = 1; j <= m; ++j) {
    e_prev[j] = std::max(e_prev[j - 1], m_prev[j - 1] - 1);
  }
  for (int i = 1; i <= n; ++i) {
    std::fill(m_cur.begin(), m_cur.end(), neg);
    std::fill(e_cur.begin(), e_cur.end(), neg);
    e_cur[0] = std::max(e_prev[0], m_prev[0] - 1);
    for (int j = 1; j <= m; ++j) {
      if (a[i - 1] == b[j - 1]) {
        m_cur[j] = std::max(m_prev[j - 1], e_prev[j - 1]) + base;
      }
      e_cur[j] = std::max({e_prev[j],
                           m_prev[j] == neg ? neg : m_prev[j] - 1,
                           e_cur[j - 1],
                           m_cur[j - 1] == neg ? neg : m_cur[j - 1] - 1});
    }
    m_prev.swap(m_cur);
    e_prev.swap(e_cur);
  }
  const std::int64_t best = std::max(m_prev[m], e_prev[m]);
  const int lcs = best <= 0 ? 0 : static_cast<int>((best + base - 1) / base);
  const int runs = static_cast<int>(static_cast<std::int64_t>(lcs) * base - best);
  *lcs_out = lcs;
  return runs;
}
