import numpy as np
import xarray as xr


def statistical_significance(score1, score2,
                             n_perm=100000):
    """
    This method computes the statistical significance of the performance
    difference between model_a and.
    Parameters
    ----------
    n_perm : int
        Number of permutations for the randomization test.
    cache : bool
        Whether to cache or not the intermediate scoring results of each model
        on each dataset. Caching enable instant access to the scores (after the
        first scoring) but coudl make use also of a huge quantity of memory.
    Returns
    -------
    stat_sig : xarray.DataArray
        A DataArray containing the statistical significance of the performance
        difference between any pair of models on the given dataset.
    """
    p1, p2 = _randomization(score1, score2, n_perm)

    return (p1, p2)


def _randomization(metric_scores_a, metric_scores_b, n_perm=100000):
    """
    This method computes the randomization test as described in [1].
    Parameters
    ----------
    metric_scores_a : numpy array
        Vector of per-query metric scores for the IR system A.
    metric_scores_b : numpy array
        Vector of per-query metric scores for the IR system B.
    n_perm : int
        Number of permutations evaluated in the randomization test.
    Returns
    -------
    metric_scores : (float, float)
        A tuple (p-value_1, p-value_2) being respectively the one-sided and two-sided p-values.
    References
    ----------
    .. [1] Smucker, Mark D., James Allan, and Ben Carterette.
        "A comparison of statistical significance tests for information retrieval evaluation."
        In Proceedings of the sixteenth ACM conference on Conference on information and knowledge management, pp. 623-632. ACM, 2007.
    """

    # find the best system
    metric_scores_a_mean = np.mean(metric_scores_a)
    metric_scores_b_mean = np.mean(metric_scores_b)

    best_metrics = metric_scores_a
    worst_metrics = metric_scores_b
    if metric_scores_a_mean < metric_scores_b_mean:
        best_metrics = metric_scores_b
        worst_metrics = metric_scores_a

    difference = np.mean(best_metrics) - np.mean(worst_metrics)
    abs_difference = np.abs(difference)

    p1 = 0.0  # one-sided
    p2 = 0.0  # two-sided
    N = float(len(metric_scores_a))

    a_sum = np.sum(best_metrics)
    b_sum = np.sum(worst_metrics)

    # repeat n_prem times
    for i in range(n_perm):
        # select a random subset
        sel = np.random.choice([False, True], len(metric_scores_a))

        a_sel_sum = np.sum(best_metrics[sel])
        b_sel_sum = np.sum(worst_metrics[sel])

        # compute avg performance of randomized models
        a_mean = (a_sum - a_sel_sum + b_sel_sum) / N
        b_mean = (b_sum - b_sel_sum + a_sel_sum) / N

        # performance difference
        delta = a_mean - b_mean

        if delta >= difference:
            p1 += 1.
        if np.abs(delta) >= abs_difference:
            p2 += 1.

    p1 /= n_perm
    p2 /= n_perm

    return p1, p2


bm25_scores = []
bm25p_scores = []

# reading data from file
with open("data_out/NDCG_10.txt", "r") as fp:
    line = fp.readline()

    while line != "":
        (bm25_score, bm25p_score) = line.split(',')

        bm25_scores.append(float(bm25_score))
        bm25p_scores.append(float(bm25p_score))
        line = fp.readline()

bm25p_scores = np.array(bm25p_scores)
bm25_scores = np.array(bm25_scores)

assert len(bm25_scores) > 0
assert len(bm25p_scores) > 0

p1, p2 = statistical_significance(bm25p_scores, bm25_scores, 100000)
print("p1 = %s and p2 = %s" % (p1, p2))
