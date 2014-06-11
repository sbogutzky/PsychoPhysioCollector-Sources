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