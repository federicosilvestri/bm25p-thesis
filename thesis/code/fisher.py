import numpy as np
import xarray as xr

def fisher_test(metric_scores_a, metric_scores_b, n_perm=100000):
    metric_scores_a_mean = np.mean(metric_scores_a)
    metric_scores_b_mean = np.mean(metric_scores_b)

    difference = np.mean(metric_scores_a) - np.mean(metric_scores_b)
    abs_difference = np.abs(difference)

    p1 = p2 = 0.0
    N = float(len(metric_scores_a))

    a_sum = np.sum(metric_scores_a)
    b_sum = np.sum(metric_scores_b)

    for i in range(n_perm):
        sel = np.random.choice([False, True], len(metric_scores_a))

        a_sel_sum = np.sum(metric_scores_a[sel])
        b_sel_sum = np.sum(metric_scores_b[sel])

        a_mean = (a_sum - a_sel_sum + b_sel_sum) / N
        b_mean = (b_sum - b_sel_sum + a_sel_sum) / N

        delta = a_mean - b_mean

        if delta >= difference:
            p1 += 1.
        if np.abs(delta) >= abs_difference:
            p2 += 1.

    p1 /= n_perm
    p2 /= n_perm

    return p1, p2
