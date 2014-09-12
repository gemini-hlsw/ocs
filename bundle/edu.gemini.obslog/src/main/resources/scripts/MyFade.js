// Created by Nick Chapman (http://chapnickman.com/)
// Information: http://chapnickman.com/2005/03/08/yellow-fade-technique/
// License: Attribution-ShareAlike 2.0 (http://creativecommons.org/licenses/by-sa/2.0/)
// instance.callback.call(this, Fader._getColor(instance.start, instance.end, instance.percent));
// Version 1.0

Fader._instances = new Array();

Fader._progressFade = function(instance)
{
	if (instance.percent < 100)
	{
		instance.percent += instance.increase;
		var color = Fader._getColor(instance.start, instance.end, instance.percent);
                //document.getElementById(instance.itemToFade).style.background = color;
                instance.itemToFade.style.background = color;

		setTimeout("Fader._progressFade(Fader._instances[" + instance.index + "])", instance.speed);
	}
	else
	{
		instance.percent = 0;
	}
}

// Code from codingforums.com
// -- http://www.codingforums.com/archive/index.php/t-4656

Fader._getColor = function (start, end, percent)
{
	var r1 = Fader._hex2dec(start.slice(0,2));
	var g1 = Fader._hex2dec(start.slice(2,4));
	var b1 = Fader._hex2dec(start.slice(4,6));

	var r2 = Fader._hex2dec(end.slice(0,2));
	var g2 = Fader._hex2dec(end.slice(2,4));
	var b2 = Fader._hex2dec(end.slice(4,6));

	var pc = percent / 100;

	r = Math.floor(r1 + (pc * (r2-r1)) + .5);
	g = Math.floor(g1 + (pc * (g2-g1)) + .5);
	b = Math.floor(b1 + (pc * (b2-b1)) + .5);

	return("#" + Fader.dec2hex(r) + Fader.dec2hex(g) + Fader.dec2hex(b));
}

Fader._hexDigit = new Array("0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F");

Fader.dec2hex = function (dec)
{
	return(Fader._hexDigit[dec>>4] + Fader._hexDigit[dec&15]);
}

Fader._hex2dec = function (hex)
{
	return(parseInt(hex,16))
}

// End codingforums.com code

Fader.xcallback = function (color) {
    alert("yes");
    //document.getElementById(itemToFade).style.background = color;
 }

function Fader(itemToFade, start, end, speed, increase)
{
	this.percent = 0;
	this.callback = null;
	this.start = null;
	this.end = null;
	this.speed = 15;
	this.increase = 1;
        this.itemToFade = itemToFade;

	if (itemToFade && start && end)
	{
		if (start.charAt(0) == "#")
			this.start = start.substring(1, start.length);
		else
			this.start = start;

		if (end.charAt(0) == "#")
			this.end = end.substring(1, end.length);
		else
			this.end = end;

		if (speed != null)
			this.speed = speed;

		if (increase != null)
			this.increase = increase;
	}

	this.index = Fader._instances.length;

	Fader._instances[this.index] = this;
}

Fader.prototype.fade = function()
{
	Fader._progressFade(this);
}
