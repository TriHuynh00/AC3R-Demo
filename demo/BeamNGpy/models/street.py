class Street:
    def __init__(self, **args):
        self.id = args.get('id')
        self.type = args.get('type')
        self.shape = args.get('shape')
        self.points = args.get('points')

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
