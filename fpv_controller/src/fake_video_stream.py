import sys
import time
import os

python_script_path = os.path.dirname(__file__)
def stream_file_limited(filename, rate=2048*16):
    with open(filename, "rb") as f:
        while chunk := f.read(rate):
            sys.stdout.buffer.write(chunk)
            sys.stdout.flush()
            time.sleep(0.1)

if __name__ == "__main__":
    raspivid_binary = python_script_path+"/raspivid.out"
    if not os.path.exists(raspivid_binary):
        raise FileNotFoundError(f"Binary stream file '{raspivid_binary}' does not exist! Please put it from https://disk.yandex.ru/d/eiMV64VesqTdpw")
    stream_file_limited(raspivid_binary)
