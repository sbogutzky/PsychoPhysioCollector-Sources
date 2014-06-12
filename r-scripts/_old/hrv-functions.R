CalculateMeanRR <- function(rr.intervals, round.digits = 4) { 
  return(round(mean(rr.intervals) * 1000, round.digits))
}

CalculateSDNN <- function(rr.intervals, round.digits = 4) {
  return(round(sd(rr.intervals) * 1000, round.digits))
}

CalculateRMSSD <- function(rr.intervals, round.digits = 4) {
  rr.differences <- abs(diff(rr.intervals))
  return(round(sqrt(sum(rr.differences^2) / length(rr.differences)) * 1000, round.digits))
}

CalculateHRVTimeDomainParameters <- function(rr.intervals) {
  hrv.time.domain.parameters <- data.frame(MeanRR = CalculateMeanRR(rr.intervals, 1), SDNN = CalculateSDNN(rr.intervals, 1), RMSSD = CalculateRMSSD(rr.intervals, 1))
  return(hrv.time.domain.parameters)
}

# CalculateHRVFrequencyDomainParameters <- function(rr.times, rr.intervals, band.vlf, band.lf, band.hf, interpolation.rate, method = c("welch", "fft"), window.width, window.overlap) {
#   
#   # Calculation of the fourier transformation required equidistant values of RR intervals
#   resampled.data <- Interpolate(rr.times, rr.intervals, interpolation.rate, "linear")
#   ts <- resampled.data$y
#   
#   if(method == "fft") {
#     
#     # Tapered using a Hanning window
#     ts <- resampled.data$y * hanning(nrow(resampled.data))
#     
#     # Zero padded to the next power of 2
#     ts <- PadZeroToPowerOfTwo(ts)
#     
#     # Smooth data
#     ts <- SmoothWithSlidingTriangularWeighting(ts)
#     
#     # Periodogram
#     periodogram <- ComputeRawPeriodogram(ts)
#   }
#   
#   if(method == "welch") {
#     
#     # Periodogram
#     periodogram <- ComputeWelchPeriodogram(ts, interpolation.rate, window.width, window.overlap)
#   }
#   
#   # Find the indexes corresponding to the VLF, LF, and HF bands
#   indexes.vlf <- (periodogram$Freq >= band.vlf[1]) & (periodogram$Freq <= band.vlf[2])
#   indexes.lf  <- (periodogram$Freq >= band.lf[1]) & (periodogram$Freq <= band.lf[2])
#   indexes.hf  <- (periodogram$Freq >= band.hf[1]) & (periodogram$Freq <= band.hf[2])
#   
#   # Find peaks
#   # VLF peak
#   freq.temp <- periodogram$Freq[indexes.vlf]
#   spec.temp <- periodogram$Spec[indexes.vlf]
#   peaks     <- FindPeaks(spec.temp)
#   if (nrow(peaks) != 0) {
#     peak.max.index  <- which(peaks$Maxima == max(peaks$Maxima))        
#     peak.vlf        <- freq.temp[peaks$Index[peak.max.index]]
#   } else {
#     peak.max.index  <- which(spec.temp == max(spec.temp))        
#     peak.vlf        <- freq.temp[peak.max.index]
#   }
#   
#   if(length(peak.vlf) == 0) peak.vlf = 0
#   
#   # LF Peak
#   freq.temp <- periodogram$Freq[indexes.lf]
#   spec.temp <- periodogram$Spec[indexes.lf]
#   peaks     <- FindPeaks(spec.temp)
#   if (nrow(peaks) != 0) {
#     peak.max.index  <- which(peaks$Maxima == max(peaks$Maxima))        
#     peak.lf         <- freq.temp[peaks$Index[peak.max.index]]
#   } else {
#     peak.max.index  <- which(spec.temp == max(spec.temp))        
#     peak.lf         <- freq.temp[peak.max.index]
#   }
#   
#   # HF Peak
#   freq.temp <- periodogram$Freq[indexes.hf]
#   spec.temp <- periodogram$Spec[indexes.hf]
#   peaks     <- FindPeaks(spec.temp)
#   if (nrow(peaks) != 0) {
#     peak.max.index  <- which(peaks$Maxima == max(peaks$Maxima))        
#     peak.hf         <- freq.temp[peaks$Index[peak.max.index]]
#   } else {
#     peak.max.index  <- which(spec.temp == max(spec.temp))        
#     peak.hf         <- freq.temp[peak.max.index]
#   }
#   
#   # Calculate raw areas (power under curve), within the freq bands (ms^2)
#   require(pracma)
#  
#   area.vlf    <- trapz(periodogram$Freq[indexes.vlf], periodogram$Spec[indexes.vlf])
#   area.lf     <- trapz(periodogram$Freq[indexes.lf], periodogram$Spec[indexes.lf])
#   area.hf     <- trapz(periodogram$Freq[indexes.hf], periodogram$Spec[indexes.hf])
#   area.total  <- area.vlf + area.lf + area.hf
#   
#   # Calculate areas relative to the total area (%)
#   p.vlf <- (area.vlf / area.total) * 100
#   p.lf  <- (area.lf / area.total)  * 100
#   p.hf  <- (area.hf / area.total) * 100
#   
#   # Calculate normalized areas (relative to HF+LF, n.u.)
#   n.lf <- area.lf / (area.lf + area.hf) * 100
#   n.hf <- area.hf / (area.lf + area.hf) * 100
#   
#   # Calculate LF/HF ratio
#   lfhf <- area.lf / area.hf
#   
#   # Create output structure
#   result.area.vlf   <- round(area.vlf * 10^6 * 100) / 100
#   result.area.lf    <- round(area.lf * 10^6 * 100) / 100
#   result.area.hf    <- round(area.hf * 10^6 * 100) / 100
#   result.area.total <- round(area.total * 10^6 * 100) / 100
#   
#   result.p.vfl      <- round(p.vlf * 100) / 100
#   result.p.lf       <- round(p.lf * 100) / 100
#   result.p.hf       <- round(p.hf * 100) / 100
#   result.n.lf       <- round(n.lf * 100) / 100
#   result.n.hf       <- round(n.hf * 100) / 100
#   result.lfhf       <- round(lfhf * 100) / 100
#   result.peak.vlf   <- round(peak.vlf * 100) / 100
#   result.peak.lf    <- round(peak.lf * 100) / 100
#   result.peak.hf    <- round(peak.hf * 100) / 100
#   
#   # Create result data frame
#   result.hrv.frequency.domain.parameters <- data.frame(AVLF = result.area.vlf, ALF = result.area.lf, AHF = result.area.hf, ATotal = result.area.total, PVLF = result.p.vfl, PLF = result.p.lf, PHF = result.p.hf, NLF = result.n.lf, NHF = result.n.hf, LFHF = result.lfhf, PeakVLF = result.peak.vlf, PeakLF = result.peak.lf, PeakHF = result.peak.hf)
#   return(result.hrv.frequency.domain.parameters)
# }

CalculateHeartRhythmCoherenceRatio <- function(time, rr.interval, sbs, sbe, p1, p2, b1, a2, plot = F) {
  
  # Interpolation
  hrv.data.interpolated <- Interpolate(time, rr.interval, 2, "linear")
  
  # Detrending
  m <- lm(hrv.data.interpolated$y ~ seq(hrv.data.interpolated$y))
  rr.intervals.detrended <- m$residuals
  
  # Hamming
  rr.intervals.hanned <- rr.intervals.detrended * hanning(length(rr.intervals.detrended))
  
  # Calculate Power Spectrum
  p <- spectrum(rr.intervals.hanned, plot = F) 
  freq <- p$freq
  spec <- p$spec
  
  # Calculate Heart Rhythm Coherence Ratio
  search.range.indexes <- (freq >= sbs) & (freq <= sbe)
  search.range.freq <- freq[search.range.indexes]
  search.range.spec <- spec[search.range.indexes]
  
  peaks <- FindPeaks(search.range.spec)
  highest.peak <- peaks[peaks$Maxima == max(peaks$Maxima),]
  highest.peak.freq <- search.range.freq[highest.peak$Index]
  
  peak.power.bound.lower <- highest.peak.freq - p1
  peak.power.bound.upper <- highest.peak.freq + p2
  
  require(pracma)
  peak.power.indexes <- (freq >= peak.power.bound.lower) & (freq <= peak.power.bound.upper)
  peak.power.area <- trapz(freq[peak.power.indexes], spec[peak.power.indexes])
  
  power.below.indexes <- (freq >= b1) & (freq < peak.power.bound.lower)
  power.below.area <- trapz(freq[power.below.indexes], spec[power.below.indexes])
  
  power.above.indexes <- (freq > peak.power.bound.upper) & (freq <= a2)
  power.above.area <- trapz(freq[power.above.indexes], spec[power.above.indexes])
  
  #ep <- peak.power.area/(power.below.area + power.above.area)
  #ep <- (peak.power.area/(power.below.area + power.above.area))^2
  #ep <- (peak.power.area/power.below.area)*(peak.power.area/power.above.area)
  ep <- peak.power.area/(power.below.area + power.above.area)
  
  if(plot) { 
    par(mfrow=c(3,1))
    
    plot(time, rr.interval, type = "o", ylab = "MILLISECONDS", xlab = "SECONDS", pch = 19)
    lines(hrv.data.interpolated$x, hrv.data.interpolated$y, type = "o", pch = 4)
    legend("bottomright", c("IBI", "INTERPOLATION VALUES"), pch = c(19, 4), inset = .02, pt.cex = 1, cex = 0.75, y.intersp = 0.5)
    
    plot(hrv.data.interpolated$x, hrv.data.interpolated$y, type = "l", ylab = "", xlab = "")
    abline(m, lwd = 2)
    legend("bottomright", c("INTERPOLATED SIGNAL", "LINIAR REGRESSION LINE"), lty = c(1, 1), lwd = c(1, 2), inset = .02, pt.cex = 1, cex = 0.75, y.intersp = 0.5)
    
    plot(hrv.data.interpolated$x, rr.intervals.hanned, type = "l", ylab = "", xlab = "")
    abline(h = 0)
    legend("bottomright", c("DETRENDED SIGNAL WITH HAMMING WINDOW"), lty = 1, lwd = 1, inset = .02, pt.cex = 1, cex = 0.75, y.intersp = 0.5)
    
    plot(freq, spec, type = "l", ylab = expression(ms^2/Hz), xlab = "FREQUENCY (HZ)")
    abline(v = sbs, lwd = 2)
    abline(v = sbe, lwd = 2)
    legend("bottomright", "SEARCH AREA", lty = 1, lwd = 2, inset = .02, pt.cex = 1, cex = 0.75, y.intersp = 0.5)
    
    plot(search.range.freq, search.range.spec, type = "l", ylab = "", xlab = "")
    points(highest.peak.freq, highest.peak$Maxima)
    abline(v = peak.power.bound.lower, lty = 2)
    abline(v = peak.power.bound.upper, lty = 2)
    legend("bottomright", "PEAK POWER", lty = 2, inset = .02, pt.cex = 1, cex = 0.75, y.intersp = 0.5)
    
    plot(freq, spec, type = "l", ylab = "", xlab = "")
    points(highest.peak.freq, highest.peak$Maxima)
    abline(v = b1, lty = 1)
    abline(v = peak.power.bound.lower, lty = 2)
    abline(v = peak.power.bound.upper, lty = 2, lwd = 2)
    abline(v = a2, lwd = 2)
    legend("bottomright", c("POWER BELOW", "POWER ABOVE") , lwd = c(1, 2), inset = .02, pt.cex = 1, cex = 0.75, y.intersp = 0.5)
  }
  return(ep)
}

Interpolate <- function(x, y, interpolation.rate, method = c("linear", "nearest", "pchip", "cubic", "spline")) {
  
  # Caluculate xi
  xi <- seq(x[1], x[length(x)], by = 1/interpolation.rate)
  
  # Interpolate
  require(signal)
  yi <- interp1(x, y, xi, method = method)
  return(data.frame(x = xi, y = yi))
}

# PadZeroToPowerOfTwo <- function(x) {
#   n <- length(x)
#   power <- ceiling(log2(n))
#   n.zeros <- 2^power
#   zeros <- matrix(0, 1, n.zeros - n)
#   return(c(x, zeros))
# }

# SmoothWithSlidingTriangularWeighting <- function(x) { 
#   i = 3
#   while(i < length(x) -1) {
#     x[i] = (x[i - 2] + 2 * x[i - 1] + 3 * x[i] + 2 * x[i + 1] + x[i + 2]) / 9
#     i = i + 1
#   }
#   return(x)
# }

# ComputeRawPeriodogram <- function(x) { 
#   spec <- abs(fft(x - mean(x)))^2 / length(x) 
#   freq <- (1:length(x) - 1) / length(x)
#   periodogram <- data.frame(Freq = freq, Spec = spec)
#   periodogram <- periodogram[1:(nrow(periodogram)/2), ]
#   return(periodogram)
# }

# ComputeWelchPeriodogram <- function(x, interpolation.rate, window.width, window.overlap) {
#   
#   # Calculate samples in DFT
#   nfft <- window.width * interpolation.rate
#     
#   # Calculte overlap
#   noverlap <- nfft * window.overlap
#   
#   # Truncate length (like Matlab)
#   trunc.x.length <- length(x) %% nfft
#   x <- x[1:(length(x) - trunc.x.length)]
#   
#   # Compute periodogram using the Welch (1967) method
#   require(oce)
#   w <- pwelch(x, noverlap = noverlap, nfft = nfft, fs = interpolation.rate, plot = F, debug = F)
#   return(periodogram <- data.frame(Freq = w$freq, Spec = w$spec))
# }

FindPeaks <- function(x) {
  
  # Find locations of local maxima
  # p = 1 at maxima, p otherwise, end point maxima excluded
  n <- length(x) - 2
  p <- sign(sign(x[2:(n + 1)] - x[3:(n + 2)]) - sign(x[1:n] - x[2:(n + 1)]) -.1) + 1
  p <- c(0, p, 0)
  
  # Indices of maxima and corresponding sample
  p <- as.logical(p) 
  i <- 1:length(p)
  return(data.frame(Index = i[p], Maxima = x[p]))
}