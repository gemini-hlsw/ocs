#!/Users/framos/opt/anaconda3/bin/python

import matplotlib.pyplot as plt
import numpy as np
import sys

if len(sys.argv) < 2:
   print(f"you have to provide the file name to plot %d ", len(sys.argv))
   sys.exit()


fig, ax = plt.subplots()
for i in range(1, len(sys.argv)):
   x, y = np.loadtxt(sys.argv[i], delimiter='\t', unpack=True)
   ax.plot(x,y, label=sys.argv[i])
   
plt.legend()
plt.show()
