NumberOfDuplicates <- function(x) {
  # Return the number of dublicates
  #   x: Vector to check.
  # Returns:
  #   Number of dublicates
  
  n <- length(x)
  x <- x[!duplicated(x)]
  m <- length(x)
  
  return(n - m)
} 

SamplingRate <- function(t) {
  # Return the sampling rate
  #   t: Vector of times in ms.
  # Returns:
  #   The sampling rate
  return(length(t) / ((t[length(t)] - t[1]) / 10^3))
}

DetectMidSwing <- function(t, x, f, c = 1.5, plot = F) {
  # Detects the gait event midswing from gyroscope data from the leg
  #
  # Args:
  #   t: Vector with intervals.
  #   x: Vector to search.
  #   f: Low pass frequency (Hz)
  #   c: Cutoff difference (s)
  # Returns:
  #   Indexes of mid swings
  
  if (plot) {
    par(mfrow = c(2, 1))
    plot(t / 1000, x, type = "l", xlab = "Time (s)", ylab = "Rotation rate X (deg/s)", xaxs = "i", yaxs = "i")
    maxima <- SearchMaxima(x)
    points(t[maxima$Index] / 1000, x[maxima$Index])
  }
  
  # Filter high frequency noise
  xf <- LPF(x, f, 2)
  xf <- (xf - mean(xf)) / sd(xf)
  
  if (plot)
    plot(t / 1000, xf, type = "l", xlab = "Time (s)", ylab = "Filtered rotation rate (deg/s)", xaxs = "i", yaxs = "i")
  
  maxima.1 <- SearchMaxima(xf)
  p <- c(0, diff(maxima.1$Maxima)) > c
  maxima.1 <- maxima.1[p & maxima.1$Maxima > 0,]
  
  if (plot)
    points(t[maxima.1$Index] / 1000, xf[maxima.1$Index])
  
  return(maxima.1$Index)
}  

LPF <- function(t, x, f) {
  # Simple low pass filter
  #
  # Args:
  #   t: Time interval between measurements (s)
  #   y: Vector to filter.
  #   
  #   f: Low pass frequency (Hz)
  # Returns:
  #   A data frame with indexes and values of local maxima.
  
  rc <- 1 / (2 * pi * f)
  a  <- t / (t + rc)
  n  <- length(x)
  xf <-x
  for(i in 2:length(x)) {
    xf[i] <- a * x[i] + (1-a) * xf[i-1]
  }
  return(xf)
} 

SearchMaxima <- function(x) {
  # Search of local maxima
  #
  # Args:
  #   x: Vector to search.
  #
  # Returns:
  #   A data frame with indexes and values of local maxima.
  
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

CalculateFlowShortScaleFactors <- function(fss.items) {
  # Calculation of FSS Factors
  #
  # Args:
  #   fss.items: Vector of FSS items.
  #
  # Returns:
  #   A data frame of FSS factors.
  
  timestamp   <- fss.items[,1]
  duration    <- fss.items[,2] - fss.items[,1]
  flow        <- fss.items[,3:12]
  fluency     <- data.frame(fss.items[,10], fss.items[,9], fss.items[,11], fss.items[,6], fss.items[,7], fss.items[,4])
  absorption  <- data.frame(fss.items[,8], fss.items[,3], fss.items[,12], fss.items[,5])
  anxiety     <- fss.items[,13:15]
  fit         <- fss.items[,16:18]
  
  fss.factors <- data.frame(
    timestamp     = timestamp,
    flow          = round(rowMeans(flow), 2), 
    sdflow        = round(apply(flow, 1, sd), 2), 
    fluency       = round(rowMeans(fluency), 2), 
    sdfluency     = round(apply(fluency, 1, sd), 2), 
    absorption    = round(rowMeans(absorption), 2), 
    sdabsorption  = round(apply(absorption, 1, sd), 2), 
    anxiety       = round(rowMeans(anxiety), 2), 
    sdanxiety     = round(apply(anxiety, 1, sd), 2), 
    fit           = round(rowMeans(fit), 2), 
    sdfit         = round(apply(fit, 1, sd), 2),
    duration      = duration
  )
  
  return(fss.factors)
}

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

CalculateHRVFrequencyDomainParameters <- function(rr.times, rr.intervals, band.range.ulf, band.range.vlf, band.range.lf, band.range.hf, band.range.vhf, interpolation.rate, window.width, window.overlap, plot = F, xlim = c(0, 0), ylim = c(0, 0)) {
  
  # Calculation of the fourier transformation required equidistant values of RR intervals
  times.series.resampled  <- Interpolate(rr.times, rr.intervals, interpolation.rate, "spline")
  rr.intervals.resampled  <- times.series.resampled$y
  
  # Periodograms
  periodogram <- ComputeWelchPeriodogram(rr.intervals.resampled, interpolation.rate, window.width, window.overlap)
  
  CalculateBandPeriodogram <- function(periodogram, band.range) {
    
    # Find the indexes corresponding to the bands
    indexes <- (periodogram$freq >= band.range[1]) & (periodogram$freq <= band.range[2])
    
    freq <- periodogram$freq[indexes]
    spec <- periodogram$spec[indexes]
    if(length(which(freq == band.range[1])) == 0) {
      indexes.lower.bound <- which(abs(periodogram$freq - band.range[1]) == min(abs(periodogram$freq - band.range[1])))
      if(length(indexes.lower.bound) == 1) {
        indexes.lower.bound <- (c(indexes.lower.bound, indexes.lower.bound + 1))
      }
      lower.bound.lm    <- lm(periodogram$spec[indexes.lower.bound] ~ periodogram$freq[indexes.lower.bound])
      lower.bound.value <- band.range[1] * lower.bound.lm$coefficients[2] + lower.bound.lm$coefficients[1]
      
      freq <- c(band.range[1], freq)
      spec <- c(lower.bound.value, spec)
    }
    
    if(length(which(freq == band.range[2])) == 0) {
      indexes.upper.bound   <- which(abs(periodogram$freq - band.range[2]) == min(abs(periodogram$freq - band.range[2])))
      if(length(indexes.upper.bound) == 1) {
        indexes.upper.bound <- (c(indexes.upper.bound, indexes.upper.bound + 1))
      }
      upper.bound.lm    <- lm(periodogram$spec[indexes.upper.bound] ~ periodogram$freq[indexes.upper.bound])
      upper.bound.value <- band.range[2] * upper.bound.lm$coefficients[2] + upper.bound.lm$coefficients[1]
      
      freq <- c(freq, band.range[2])
      spec <- c(spec, upper.bound.value)
    }
    
    return(periodogram <- data.frame(freq, spec))
  }
  
  periodogram.ulf   <- CalculateBandPeriodogram(periodogram, band.range.ulf)
  periodogram.vlf   <- CalculateBandPeriodogram(periodogram, band.range.vlf)
  periodogram.lf    <- CalculateBandPeriodogram(periodogram, band.range.lf)
  periodogram.hf    <- CalculateBandPeriodogram(periodogram, band.range.hf)
  periodogram.vhf   <- CalculateBandPeriodogram(periodogram, band.range.vhf)
  
  # Plot
  if (plot) {
    par(mar = c(5.1, 5.1, 4.1, 2.1))
    plot(periodogram$freq, periodogram$spec, type = "l", xlab = "Frequency (Hz)", ylab = expression(PSD~(s^2/Hz)), xaxs = "i", yaxs = "i", xlim = xlim, ylim = ylim, xaxt = "n")
    axis(side = 1, at = c(band.range.vlf[1], band.range.vlf[2], band.range.lf[2], band.range.hf[2], 0.5, 1.0, 2.0))
    title(main = "FFT spectrum", sub = "Welch's periodogram: 256 s window with 50% overlap")
    
    polygon(c(periodogram.ulf$freq[1], periodogram.ulf$freq, periodogram.ulf$freq[length(periodogram.ulf$freq)]), c(0, periodogram.ulf$spec, 0), col = terrain.colors(6)[6])
    polygon(c(periodogram.vlf$freq[1], periodogram.vlf$freq, periodogram.vlf$freq[length(periodogram.vlf$freq)]), c(0, periodogram.vlf$spec, 0), col = terrain.colors(6)[5]) 
    polygon(c(periodogram.lf$freq[1], periodogram.lf$freq, periodogram.lf$freq[length(periodogram.lf$freq)]), c(0, periodogram.lf$spec, 0), col = terrain.colors(6)[4]) 
    polygon(c(periodogram.hf$freq[1], periodogram.hf$freq, periodogram.hf$freq[length(periodogram.hf$freq)]), c(0, periodogram.hf$spec, 0), col = terrain.colors(6)[3])
    polygon(c(periodogram.vhf$freq[1], periodogram.vhf$freq, periodogram.vhf$freq[length(periodogram.vhf$freq)]), c(0, periodogram.vhf$spec, 0), col = terrain.colors(6)[2])
  }
  
  CalculatePeak <- function(freq, spec) {
    maxima <- SearchMaxima(periodogram.lf$spec)
    if (nrow(maxima) != 0) {
      peak.index  <- which(maxima$Maxima == max(maxima$Maxima))        
      peak.freq   <- freq[maxima$Index[peak.index]]
    } else {
      peak.index  <- which(spec == max(spec))        
      peak.freq   <- freq[peak.index]
    }
  }

  # Find peaks
  peak.vlf  <- CalculatePeak(periodogram.vlf$freq, periodogram.vlf$spec)
  peak.lf   <- CalculatePeak(periodogram.lf$freq, periodogram.lf$spec)
  peak.hf   <- CalculatePeak(periodogram.hf$freq, periodogram.hf$spec)
  
  # Calculate raw areas (power under curve), within the freq bands (ms^2)
  require(pracma)
 
  area.vlf    <- trapz(periodogram.vlf$freq, periodogram.vlf$spec)
  area.lf     <- trapz(periodogram.lf$freq, periodogram.lf$spec)
  area.hf     <- trapz(periodogram.hf$freq, periodogram.hf$spec)
  area.total  <- area.vlf + area.lf + area.hf
  
  # Calculate areas relative to the total area (%)
  p.vlf <- (area.vlf / area.total) * 100
  p.lf  <- (area.lf / area.total)  * 100
  p.hf  <- (area.hf / area.total) * 100
  
  # Calculate normalized areas (relative to HF+LF, n.u.)
  n.lf <- area.lf / (area.lf + area.hf) * 100
  n.hf <- area.hf / (area.lf + area.hf) * 100
  
  # Calculate LF/HF ratio
  lfhf <- area.lf / area.hf
  
  # Create output structure
  result.area.vlf   <- round(area.vlf * 10^6 * 100) / 100
  result.area.lf    <- round(area.lf * 10^6 * 100) / 100
  result.area.hf    <- round(area.hf * 10^6 * 100) / 100
  result.area.total <- round(area.total * 10^6 * 100) / 100
  
  result.p.vfl      <- round(p.vlf * 1000) / 1000
  result.p.lf       <- round(p.lf * 1000) / 1000
  result.p.hf       <- round(p.hf * 1000) / 1000
  result.n.lf       <- round(n.lf * 100) / 100
  result.n.hf       <- round(n.hf * 100) / 100
  result.lfhf       <- round(lfhf * 100) / 100
  result.peak.vlf   <- round(peak.vlf * 1000) / 1000
  result.peak.lf    <- round(peak.lf * 1000) / 1000
  result.peak.hf    <- round(peak.hf * 1000) / 1000
  
  if(plot) {
    legend("topright", title = "Frequency bands", c("ULF", paste("VLF: ", result.area.vlf, " ms^2"), paste("LF: ", result.area.lf, " ms^2"), paste("HF: ", result.area.hf, " ms^2"), "VHF", paste("LF/HF: ", result.lfhf)), fill = rev(terrain.colors(6)), inset = .05, cex = .7)
  }
  
  # Create result data frame
  result.hrv.frequency.domain.parameters <- data.frame(vlfpeakfft = result.peak.vlf, lfpeakfft = result.peak.lf, hfpeakfft = result.peak.hf, vlfpowfft = result.area.vlf, lfpowfft = result.area.lf, hfpowfft = result.area.hf, vlfprfft = result.p.vfl, lfprfft = result.p.lf, hfprfft = result.p.hf, lfnufft = result.n.lf, lfnufft = result.n.hf, totpowfft = result.area.total, lfhffft = result.lfhf)
  return(result.hrv.frequency.domain.parameters)
}

Interpolate <- function(t, y, interpolation.rate, method = c("linear", "nearest", "pchip", "cubic", "spline")) {
  
  # Caluculate xi
  ti <- seq(t[1], t[length(t)], by = 1/interpolation.rate)
  
  # Interpolate
  require(signal)
  yi <- interp1(t, y, ti, method = method)
  return(data.frame(t = ti, y = yi))
}

ComputeWelchPeriodogram <- function(x, interpolation.rate, window.width, window.overlap) {
  
  # Calculate samples in DFT
  nfft <- window.width * interpolation.rate
    
  # Calculte overlap
  noverlap <- nfft * window.overlap
  
  # Truncate length (like Matlab)
  trunc.x.length <- length(x) %% noverlap
  x <- x[1:(length(x) - trunc.x.length)]
  
  # Compute periodogram using the Welch (1967) method
  require(oce)
  w <- pwelch(x, noverlap = noverlap, nfft = nfft, fs = interpolation.rate, plot = F, debug = F)
  return(periodogram <- data.frame(freq = round(w$freq, 3), spec = w$spec))
}

PlotRelation <- function(x, y, ..., summary = F) {
  
  if(length(x) > 1) {
    
    # Plot x
    plot(x, y, ...)
    
    # Plot lineare Beziehung
    linear.relation <- lm(y ~ x)
    abline(linear.relation, ...)
    if(summary)
      print(summary(linear.relation))
    
    linear.adj.r.squared <- round(summary(linear.relation)$adj.r.squared, 3)
    linear.p <- summary(linear.relation)$sigma
    linear.sig <- ""
    if (linear.p < .1)
      linear.sig <- "."
    if (linear.p < .05)
      linear.sig <- "*"
    if (linear.p < .01)
      linear.sig <- "**"
    if (linear.p < .001)
      linear.sig <- "***"
    
    # Plot quadratische Beziehung
    quadratic.relation <- lm(y ~ x + I(x^2))
    xq <- seq(min(x, na.rm = T) - 10, max(x, na.rm = T) + 10, len = 200)
    yq <- quadratic.relation$coefficients %*% rbind(1, xq, xq^2)
    lines(xq, yq, lty = 2, ...)
    if(summary)
      print(summary(quadratic.relation))
    
    quadratic.adj.r.squared <- round(summary(quadratic.relation)$adj.r.squared, 3)
    quadratic.p <- summary(quadratic.relation)$sigma
    quadratic.sig <- ""
    if (quadratic.p < .1)
      quadratic.sig <- "."
    if (quadratic.p < .05)
      quadratic.sig <- "*"
    if (quadratic.p < .01)
      quadratic.sig <- "**"
    if (quadratic.p < .001)
      quadratic.sig <- "***"
    title(sub = bquote({R[linear]}^2 ~ "=" ~ .(linear.adj.r.squared) ~ .(linear.sig) ~ "   " ~ {R[quadratisch]}^2 ~ "=" ~ .(quadratic.adj.r.squared) ~ .(quadratic.sig)), line = 4)
  }
}