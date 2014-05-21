setwd("~/Entwicklung/projects/bogutzky/repositories/data-collector-android/r-scripts")

directory.name      <- "2014-05-17_17-25-09"
sensor.bc98.subset  <- read.csv(paste("../data/", directory.name, "/sensor_BC98_subset_3.csv", sep =""))
summary(sensor.bc98.subset)

m <- 2000
n <- 2225
t <- sensor.bc98.subset$Timestamp[m:n] - sensor.bc98.subset$Timestamp[m]
gx <- sensor.bc98.subset$Gyroscope.X[m:n]

plot(t, gx, type = "l", xlab = "ms", ylab = "m/s^2")
maxima <- SearchMaxima(gx)
points(t[maxima$Index], gx[maxima$Index])

# Filter high frequency noise
gx.1 <- LPF(gx, 1/56, 2)
plot(t, gx.1, type = "l", xlab = "ms", ylab = "m/s^2")
maxima.1 <- SearchMaxima(gx.1)
points(t[maxima.1$Index], gx.1[maxima.1$Index])

require(TSA)
periodogram(gx)
periodogram(gx.1)

LPF <- function(y, t, f) {
  # Simple low pass filter
  #
  # Args:
  #   y: Vector to filter.
  #   t: Time interval between measurements (s)
  #   f: Low pass frequency (Hz)
  # Returns:
  #   A data frame with indexes and values of local maxima.
  
  rc <- 1 / (2 * pi * f)
  a  <- t / (t + rc)
  n  <- length(y)
  yf <- y
  for(i in 2:length(y)) {
    yf[i] <- a * y[i] + (1-a) * yf[i-1]
  }
  return(yf)
} 

SearchMaxima <- function(y) {
  # Search of local maxima
  #
  # Args:
  #   y: Vector to search.
  #
  # Returns:
  #   A data frame with indexes and values of local maxima.
  
  # Find locations of local maxima
  # p = 1 at maxima, p otherwise, end point maxima excluded
  n <- length(y) - 2
  p <- sign(sign(y[2:(n + 1)] - y[3:(n + 2)]) - sign(y[1:n] - y[2:(n + 1)]) -.1) + 1
  p <- c(0, p, 0)
  
  # Indices of maxima and corresponding sample
  p <- as.logical(p) 
  i <- 1:length(p)
  return(data.frame(Index = i[p], Maxima = y[p]))
}

CalculateJerk <- function(t, x) {
  # Computes the jerk an acceleration.
  #
  # Args:
  #   t: Vectors with intervals.
  #   x: The other vector with acceleration. t and x must have the same length, greater than one,
  #      with no missing values.
  #
  # Returns:
  #   The jerk of the acceleration.
  
  jerk <- c()
  for(i in 1:length(t)) {
    if(i < 2) {
      jerk <- c(jerk, 0)
    } else {
      j     <- (x[i] - x[i - 1]) / (t[i] - t[i - 1])
      jerk  <- c(jerk, j)
    }
  }
  return(jerk)
}

CalculateJerkCost(t, x, y, z) {
  # Computes the jerk cost of an acceleration in x-, y- and z-direction.
  #
  # Args:
  #   t: Vectors with intervals.
  #   x: The other vector with acceleration. t and x must have the same length, greater than one,
  #      with no missing values.
  #   y: The other vector with acceleration. t and y must have the same length, greater than one,
  #      with no missing values.
  #   z: The other vector with acceleration. t and z must have the same length, greater than one,
  #      with no missing values.
  #
  # Returns:
  #   The jerk cost of the acceleration.
  
  require(pracma)
  jerk.x    <- CalculateJerk(t, x)
  jerk.y    <- CalculateJerk(t, y)
  jerk.z    <- CalculateJerk(t, z)
  jerk.cost <- 1/2 * trapz(t, jerk.x^2 + jerk.y^2 + jerk.z^2)
  return(jerk.cost)
}