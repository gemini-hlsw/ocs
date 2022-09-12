import math
import numpy
from matplotlib import pyplot

numX = 8
numY = 9
#IFU_DIAMETER = 0.144
#IFU_DIAMETER = 0.144
#IFU_DIAMETER = 0.288
#IFU_DIAMETER = 0.48  # SR good Venu.
IFU_DIAMETER = 0.288  # HR good Venu
#IFU_SPACING = 0.04
IFU_SPACING = 0.00
distX = math.sqrt(3.0)/2.0 * (IFU_DIAMETER + IFU_SPACING)
distY = (IFU_DIAMETER + IFU_SPACING) / 2.0
x = []
y = []
for i in range(numX):
    for j in range(numY):
        x.append((i - numX/2.0) * distX)
        y.append((2*j - numY + abs(i)%2 - 1) * distY)
x = numpy.array(x)
y = numpy.array(y)
r = numpy.sqrt(x**2 + y**2)

#R = 1.7  # standard resolution IFU
#R = 0.333378283  # standard resolution IFU
#R = 0.577  # standard resolution IFU
#R = 0.577  # standard resolution IFU
R =  0.0571477  # standard resolution IFU
#R = 0.66  # standard resolution IFU good Venu
#R = 0.659 # high resolution IFU good   Venu
pyplot.figure(figsize=(5, 5))
pyplot.plot(x, y, marker='.', markersize=1, linestyle='', color='black')
pyplot.plot(x[r>R], y[r>R], marker='H', markersize=30, linestyle='', color='black', alpha=0.6)
pyplot.plot(x[r<=R], y[r<=R], marker='H', markersize=30, linestyle='', color='green', alpha=0.6)
pyplot.plot([0], [0], marker='.', color='red')
pyplot.gca().add_patch(pyplot.Circle((0, 0), radius=R, color='red', Fill=False))
pyplot.axis('equal')
pyplot.grid(linewidth=0.25)
pyplot.title('GHOST Standard Resolution IFU')

'''
R = 2.8  # high-resolution IFU
pyplot.figure(figsize=(5, 5))
pyplot.plot(x, y, marker='.', markersize=1, linestyle='', color='black')
pyplot.plot(x[r>R], y[r>R], marker='H', markersize=30, linestyle='', color='black', alpha=0.6)
pyplot.plot(x[r<=R], y[r<=R], marker='H', markersize=30, linestyle='', color='green', alpha=0.6)
pyplot.plot([0], [0], marker='.', color='red')
pyplot.gca().add_patch(pyplot.Circle((0, 0), radius=R, color='red', Fill=False))
pyplot.axis('equal')
pyplot.grid(linewidth=0.25)
pyplot.title('GHOST High Resolution IFU')
'''
pyplot.show()
