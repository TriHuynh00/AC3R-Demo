from typing import Optional, List, Dict
import numpy as np
import matplotlib.pyplot as plt

NUM_CLUSTERS = 3  # LOW MED HIGH


class KMeans:
    """
    The KMean class declares the interface that perform K-Means clustering.

    Args:
        n_clusters (int): The number of clusters to form as well as the number of centroids to generate.
        data (np.array): Training instances to cluster.
    """

    def __init__(self, data: List[float], n_clusters: Optional[int] = None):
        self.data = list(set(data))  # Remove duplicates from list
        self.n_clusters = NUM_CLUSTERS if n_clusters is None else n_clusters
        self.model = None

        # Validate the input data
        self._validate_input()

        # Compute k-means model
        self._set_km_model()

    def _set_km_model(self) -> None:
        """
        Compute k-means model.

        """
        from sklearn.cluster import KMeans
        data = np.array(self.data).reshape(-1, 1)
        self.model = KMeans(n_clusters=NUM_CLUSTERS, init='k-means++', random_state=0).fit(data)

    def _validate_input(self) -> None:
        """
        Validate input data for k-means model.

        """
        if len(self.data) < self.n_clusters:
            raise Exception(f'Exception: Number of data points is smaller than n_clusters ({self.n_clusters})!')
        if len(self.data) > 0 and all(elem == self.data[0] for elem in self.data) is True:
            raise Exception("Exception: All elements in list are equal!")

    def get_clusters(self, debug: Optional[bool] = False) -> Dict[str, list]:
        """
        Compute k-means clustering.

        Returns:
            km_dict dict[str, list]: e.g. {'LOW': [], 'MED': [], 'HIGH': []}
        """

        # Categorizing data points following low - med - high
        km_dict = {"LOW": [], "MED": [], "HIGH": []}
        data = np.array(self.data).reshape(-1, 1)
        centers = self.model.cluster_centers_
        labels = self.model.predict(sorted(centers.tolist()))  # low, med, high ids
        for point in data:
            label = self.model.predict(np.array(point).reshape(-1, 1))
            if label == labels[0]:  # LOW
                km_dict["LOW"].append(point[0])
            elif label == labels[1]:  # MED
                km_dict["MED"].append(point[0])
            else:  # HIGH
                km_dict["HIGH"].append(point[0])

        # Debug mode
        if debug:
            plt.scatter(data[:, 0], np.zeros(len(data)))
            plt.scatter(centers[:, 0], np.zeros(len(centers)))
            plt.show()
        return {k: sorted(km_dict[k]) for k in km_dict}
