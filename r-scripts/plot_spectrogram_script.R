require(RHRV)

hrv.data <- CreateHRVData()
hrv.data <- SetVerbose(hrv.data, TRUE)
# hrv.data <- LoadBeatAscii(hrv.data, "2014-05-22_17-12-49-subset-2-step-intervals.txt", RecordPath = "../data/")
hrv.data <- LoadBeatAscii(hrv.data, "2014-06-06_10-49-56-subset-3-step-intervals.txt", RecordPath = "../data/")
hrv.data <- BuildNIHR(hrv.data)
PlotNIHR(hrv.data)

hrv.data <- EditNIHR(hrv.data)
PlotNIHR(hrv.data)

hrv.data <- InterpolateNIHR (hrv.data, freqhr = 2)
PlotHR(hrv.data)

PlotSpectrogram(hrv.data, size = 128, shift = 64, freqRange = c(0.03, 0.4))

hrv.data <- CreateFreqAnalysis(hrv.data)
hrv.data <- CalculatePowerBand(hrv.data, indexFreqAnalysis = 1, type = "wavelet", wavelet = "d4", ULFmin = 0, ULFmax = 0.01, VLFmin = 0.01, VLFmax = 0.03, LFmin = 0.03, LFmax = 0.15, HFmin = 0.15, HFmax = 0.4)
PlotPowerBand(hrv.data, indexFreqAnalysis = 1, normalized = F, hr = T)