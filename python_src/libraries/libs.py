import json
import os
from pathlib import Path
from typing import List
import scipy.stats as stats
from bisect import bisect_left

ROOT: Path = Path(os.path.abspath(os.path.join(os.path.dirname(__file__))))
PATH_TEST = str(ROOT.joinpath("../tests"))
PATH_DATA = str(ROOT.joinpath("../data"))


def _collect_police_report(path):
    with open(path) as file:
        report_data = json.load(file)
    return report_data


def _VD_A(treatment: List[float], control: List[float]):
    m = len(treatment)
    n = len(control)
    if m != n:
        raise ValueError("Data d and f must have the same length")
    r = stats.rankdata(treatment + control)
    r1 = sum(r[0:m])
    # Compute the measure
    # A = (r1/m - (m+1)/2)/n # formula (14) in Vargha and Delaney, 2000
    A = (2 * r1 - m * (m + 1)) / (2 * n * m)  # equivalent formula to avoid accuracy errors
    levels = [0.147, 0.33, 0.474]  # effect sizes from Hess and Kromrey, 2004
    # magnitude = ["negligible", "small", "medium", "large"]
    # scaled_A = (A - 0.5) * 2
    # magnitude = magnitude[bisect_left(levels, abs(scaled_A))]
    estimate = A
    return estimate
