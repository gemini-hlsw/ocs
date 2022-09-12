import math
import numpy
from matplotlib import pyplot

numX = 8
numY = 9
IFU_DIAMETER = 0.24  # HR 
#IFU_DIAMETER = 0.27713  # HR 
#IFU_SPACING = 0.02
IFU_SPACING = 0.0
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

#R =  0.3409428  # standard resolution IFU
#R =  0.33339077055  # standard resolution IFU
R =  0.34  # standard resolution IFU
pyplot.figure(figsize=(5, 5))
pyplot.plot(x, y, marker='.', markersize=1, linestyle='', color='black')
pyplot.plot(x[r>R], y[r>R], marker='H', markersize=30, linestyle='', color='black', alpha=0.6)
pyplot.plot(x[r<=R], y[r<=R], marker='H', markersize=30, linestyle='', color='green', alpha=0.6)
pyplot.plot([0], [0], marker='.', color='red')
pyplot.gca().add_patch(pyplot.Circle((0, 0), radius=R, color='red', Fill=False))
pyplot.axis('equal')
pyplot.grid(linewidth=0.25)
pyplot.title('GHOST Standard Resolution IFU')

#IFU_DIAMETER = 0.16628  # HR 
IFU_DIAMETER = 0.144  # HR 
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

#R =  0.3409428  # standard resolution IFU
R =  0.34  # standard resolution IFU
#R =  0.3295625292  # standard resolution IFU
#R =  0.3315625294669713  # standard resolution IFU
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
