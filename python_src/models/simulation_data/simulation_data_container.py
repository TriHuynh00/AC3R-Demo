class SimulationDataContainer:
    def __init__(self, debug: bool = False):
        self.debug = debug
        self.simulations = []

    def start(self):
        if self.debug is True:
            for sim_data_collector in self.simulations:
                sim_data_collector.get_simulation_data().start()

    def save(self):
        if self.debug is True:
            for sim_data_collector in self.simulations:
                sim_data_collector.save()

    def end(self, success: bool, exception=None):
        if self.debug is True:
            for sim_data_collector in self.simulations:
                sim_data_collector.get_simulation_data().end(success=success, exception=exception)

    def append(self, sim_data_collector):
        if self.debug is True:
            self.simulations.append(sim_data_collector)

    def collect(self):
        if self.debug is True:
            for sim_data_collector in self.simulations:
                sim_data_collector.collect_current_data()