"""
Just a small script to test our API socket
"""

import json
import socket

from time import sleep

class DrivingAPI:


    def __init__(self, host, port):
        print("Connecting to API socket...")
        self.__sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.__sck.connect((host, port))
        print("Connected!")

    def send(self, msg):
        self.__sck.send(msg)

    def send_str(self, msg):
        print('Sending string over socket: {}'.format(msg))
        self.send((msg + '\n').encode('ascii'))

    def receive(self):
        buffer = ''
        data = True
        while data:
            data = self.__sck.recv(4096)
            data = data.decode('ascii')
            buffer += data
            while buffer.find('\n') != -1:
                line, buffer = buffer.split('\n', 1)
                yield line

        return

    def steer(self, steering):
        self.send_str('steer:{}'.format(steering))

    def throttle(self, throttle):
        self.send_str('throttle:{}'.format(throttle))

    def get_steering(self):
        self.send_str('getsteering')

    def get_throttle(self):
        self.send_str('getthrottle')



api = DrivingAPI('127.0.0.1', 23512)
sleep(3)
api.throttle(2)
sleep(2)
api.get_throttle()
for line in api.receive():
    print(line)
