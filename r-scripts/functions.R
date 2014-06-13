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

CalculateHRVFrequencyDomainParameters <- function(rr.times, rr.intervals, band.vlf, band.lf, band.hf, interpolation.rate, window.width, window.overlap) {
  
  # Calculation of the fourier transformation required equidistant values of RR intervals
  resampled.data          <- Interpolate(rr.times, rr.intervals, interpolation.rate, "linear")
  resampled.rr.intervals  <- resampled.data$y
  print(length(resampled.rr.intervals))
  
  # Periodogram
  periodogram <- ComputeWelchPeriodogram(resampled.rr.intervals, interpolation.rate, window.width, window.overlap)

  return(periodogram)
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
}

Interpolate <- function(x, y, interpolation.rate, method = c("linear", "nearest", "pchip", "cubic", "spline")) {
  
  # Caluculate xi
  xi <- seq(x[1], x[length(x)], by = 1/interpolation.rate)
  
  # Interpolate
  require(signal)
  yi <- interp1(x, y, xi, method = method)
  return(data.frame(x = xi, y = yi))
}

ComputeWelchPeriodogram <- function(x, interpolation.rate, window.width, window.overlap) {
  
  # Calculate samples in DFT
  nfft <- window.width * interpolation.rate
  print(nfft)
    
  # Calculte overlap
  noverlap <- nfft * window.overlap
  print(noverlap)
  
  # Truncate length (like Matlab)
  trunc.x.length <- length(x) %% noverlap
  print(trunc.x.length)
  x <- x[1:(length(x) - trunc.x.length)]
  
  # Compute periodogram using the Welch (1967) method
  require(oce)
  print(summary(x))
  w <- pwelch(x, noverlap = noverlap, nfft = nfft, fs = interpolation.rate, plot = T, debug = T)
  #w <- pwelch(x, 7, .5, fs = interpolation.rate, plot = T, debug = T)
  return(periodogram <- data.frame(Freq = w$freq, Spec = w$spec))
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