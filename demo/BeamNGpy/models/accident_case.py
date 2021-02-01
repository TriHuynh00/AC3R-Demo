class AccidentCase:
    def __init__(self, **args):
        # self.road_list = args.get('road_list')
        self.crash_point = args.get('crash_point')
        self.name = args.get('name')
        self.street_list = args.get('street_list')
        self.car_list = args.get('car_list')

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)