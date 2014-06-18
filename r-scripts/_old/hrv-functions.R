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
