class Street:
    def __init__(self, **args):
        self.id = args.get('id')
        self.type = args.get('type')
        self.shape = args.get('shape')
        self.points = args.get('points')

    def get_road_test(self):
        start_point = self.points[0]
        end_point = self.points[-1]
        if start_point[0] == end_point[0]:
            return {
                "start_point": start_point[1],
                "end_point": end_point[1],
                "constant": start_point[0],
                "type": "vertical"
            }
        elif start_point[1] == end_point[1]:
            return {
                "start_point": start_point[0],
                "end_point": end_point[0],
                "constant": start_point[1],
                "type": "horizontal"
            }
        return None


    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
