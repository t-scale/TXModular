// Copyright (C) 2010  Paul Miller. This file is part of TX Modular system distributed under the terms of the GNU General Public License (see file LICENSE).

TXEQShelfSt : TXModuleBase {		// hi/lo shelf eq module

	classvar <>classData;

	*initClass{
		//	set class specific variables
		classData = ();
		classData.arrInstances = [];
		classData.defaultName = "EQ Shelf St";
		classData.moduleRate = "audio";
		classData.moduleType = "insert";
		classData.noInChannels = 2;
		classData.arrCtlSCInBusSpecs = [
			["Frequency", 1, "modfreq", 0],
			["Smooth Time", 1, "modLag", 0],
			["Cut-Boost", 1, "modcutboost", 0],
			["Dry-Wet Mix", 1, "modWetDryMix", 0]
		];
		classData.noOutChannels = 2;
		classData.arrOutBusSpecs = [
			["Out L + R", [0,1]],
			["Out L only", [0]],
			["Out R only", [1]]
		];
		classData.arrFreqRangePresets = TXFilter.arrFreqRanges;
		classData.arrGainRangePresets = TXFilter.arrGainRanges;
	}

	*new{ arg argInstName;
		^super.new.init(argInstName);
	}

	init {arg argInstName;
		//	set  class specific instance variables
		arrSynthArgSpecs = [
			["in", 0, 0],
			["out", 0, 0],
			["freq", 0.5, defLagTime],
			["freqMin",40, defLagTime],
			["freqMax", 10000, defLagTime],
			["lag", 0.5, defLagTime],
			["lagMin", 0.01, defLagTime],
			["lagMax", 1, defLagTime],
			["cutboost", 0.25, defLagTime],
			["cutboostMin", -18,  defLagTime],
			["cutboostMax", 18, defLagTime],
			["wetDryMix", 1.0, defLagTime],
			["modfreq", 0, defLagTime],
			["modLag", 0, defLagTime],
			["modcutboost", 0, defLagTime],
			["modWetDryMix", 0, defLagTime],
		];
		arrOptions = [0, 0];
		arrOptionData = [
			[
				["Resonant Low Shelf Biquad",
					{arg inSound, inFreq, indb; BLowShelf.ar(inSound, inFreq, 1, indb); }
				],
				["Resonant High Shelf Biquad",
					{arg inSound, inFreq, indb; BHiShelf.ar(inSound, inFreq, 1, indb); }
				],
			],
			[
				["None",
					{arg input, lagtime; input;}
				],
				["Linear",
					{arg input, lagtime; Ramp.kr(input, lagtime); }
				],
				["Exp 1",
					{arg input, lagtime; Lag.kr(input, lagtime); }
				],
				["Exp 2",
					{arg input, lagtime; Lag2.kr(input, lagtime); }
				],
				["Exp 3",
					{arg input, lagtime; Lag3.kr(input, lagtime); }
				],
			]
		];
		synthDefFunc = { arg in, out, freq, freqMin, freqMax, lag, lagMin, lagMax,
			cutboost, cutboostMin, cutboostMax,
			wetDryMix, modfreq = 0.0, modLag = 0.0, modcutboost = 0.0, modWetDryMix = 0.0;
			var input, outFunction, lagFunction, outFilter, outClean, sumfreq, sumlag, sumcutboost, mixCombined;
			var startEnv = TXEnvPresets.startEnvFunc.value;

			input = TXClean.ar(startEnv * InFeedback.ar(in, 2));
			sumfreq = ( (freqMax/ freqMin) ** ((freq + modfreq).max(0.001).min(1)) ) * freqMin;
			sumlag = ( (lagMax/lagMin) ** ((lag + modLag).max(0.001).min(1)) ) * lagMin;
			sumcutboost =  cutboostMin
			+ ( (cutboostMax - cutboostMin) * (cutboost + modcutboost).max(0).min(1) );
			mixCombined = (wetDryMix + modWetDryMix).max(0).min(1);
			outFunction = arrOptionData.at(0).at(arrOptions.at(0)).at(1);
			lagFunction = arrOptionData.at(1).at(arrOptions.at(1)).at(1);
			outFilter = outFunction.value(
				input,
				lagFunction.value(sumfreq, sumlag),
				sumcutboost
			);
			outFilter = LeakDC.ar(outFilter);
			Out.ar(out, (TXClean.ar(startEnv * outFilter) * mixCombined) + (input * (1-mixCombined)) );
		};
		guiSpecArray = [
			["SynthOptionPopupPlusMinus", "Filter", arrOptionData, 0],
			["SpacerLine", 6],
			["TXMinMaxSliderSplit", "Frequency", ControlSpec(0.midicps, 20000, \exponential),
				"freq", "freqMin", "freqMax", nil, classData.arrFreqRangePresets],
			["SpacerLine", 6],
			["SynthOptionPopupPlusMinus", "Smoothing", arrOptionData, 1],
			["SpacerLine", 6],
			["TXMinMaxSliderSplit", "Smooth time", ControlSpec(0.0001, 30, \exp, 0, 1, units: " secs"),
				"lag", "lagMin", "lagMax"],
			["DividingLine"],
			["SpacerLine", 6],
			["TXMinMaxSliderSplit", "Cut-Boost", ControlSpec(-48, 18), "cutboost", "cutboostMin", "cutboostMax",
				nil, classData.arrGainRangePresets],
			["DividingLine"],
			["SpacerLine", 6],
			["WetDryMixSlider"],
		];
		arrActionSpecs = this.buildActionSpecs(guiSpecArray);
		//	use base class initialise
		this.baseInit(this, argInstName);
		//	load the synthdef and create the synth
		this.loadAndMakeSynth;
	}

}
