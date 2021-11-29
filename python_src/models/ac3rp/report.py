PART_DICT = {
    "front": 'F',
    "side": '',
    "left": 'L',
    "right": 'R',
    "rear": 'B',
    "back": 'B',
    "middle": 'M',
    "any": "ANY"
}


class Report:
    @staticmethod
    def from_dict(report_dict):
        parts = []
        for part in report_dict["parts"]:
            part_name = part["name"]
            if len(part_name.split()) < 3:
                parts.append({
                    "name": "".join(PART_DICT[p] for p in part_name.split()),
                    "damage": part["damage"]
                })
            else:
                words = part_name.split()
                k = []
                for i in words:
                    if part_name.count(i) > 1 and (i not in k) or part_name.count(i) == 1:
                        k.append(i)
                if len(k) == 2:  # (side left left)
                    parts.append({
                        "name": "".join(PART_DICT[p] for p in k),
                        "damage": part["damage"]
                    })
                else:  # (front left right) or (side left right)
                    parts.append({
                        "name": "".join(PART_DICT[p] for p in [k[0], k[1]]),  # (front left) or (side left)
                        "damage": part["damage"]
                    })
                    parts.append({
                        "name": "".join(PART_DICT[p] for p in [k[0], k[2]]),  # (front right) or (side right)
                        "damage": part["damage"]
                    })
        return Report(report_dict["name"], parts)

    def __init__(self, name: str, parts: []):
        self.name = name
        self.parts = parts
