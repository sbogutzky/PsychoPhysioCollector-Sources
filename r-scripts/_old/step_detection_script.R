DetectMidSwing <- function(t, x, f, c = 1.5, plot = F) {
  # Detects the gait event midswing from gyroscope data from the leg
  #
  # Args:
  #   x: Vector to search.
  #   t: Vector with intervals.
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
  #   t: Vector with intervals.
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

CalculateJerkCost <- function(t, x, y, z, plot = F) {
  # Computes the jerk cost of an acceleration in x-, y- and z-direction.
  #
  # Args:
  #   t: Vectors with intervals (ms).
  #   x: The other vector with acceleration. t and x must have the same length, greater than one,
  #      with no missing values.
  #   y: The other vector with acceleration. t and y must have the same length, greater than one,
  #      with no missing values.
  #   z: The other vector with acceleration. t and z must have the same length, greater than one,
  #      with no missing values.
  #
  # Returns:
  #   The jerk cost of the acceleration.
  
  jerk.x <- CalculateJerk(t, x)
  jerk.y <- CalculateJerk(t, y)
  jerk.z <- CalculateJerk(t, z)
  
  if (plot) {
    par(mfrow = c(3,1), mgp = c(2, 1, 0)) 
  
    plot(t / 1000, y, type = "l", xlab = "Time (s)", ylab = expression(Acceleration ~ (m/s^2)), xaxs = "i", yaxs = "i")
    plot(t / 1000, jerk.y, type = "l", xlab = "Time (s)", ylab = expression(Jerk ~ (m/s^3)), xaxs = "i", yaxs = "i")
    plot(t / 1000, 1/2*(jerk.x^2 + jerk.y^2 + jerk.z^2), type = "l", xlab = "Time (s)", ylab = expression(1/2 %*% (Jerk[x]^2 ~ (t)+Jerk[y]^2 ~ (t)+Jerk[z]^2 ~ (t))), xaxs = "i", yaxs = "i")
  }
  
  require(pracma)
  jerk.cost <- 1/2 * trapz(t, jerk.x^2 + jerk.y^2 + jerk.z^2)
  return(jerk.cost)
}

setwd("~/Entwicklung/projects/bogutzky/repositories/data-collector-android/r-scripts")

directory.name      <- "2014-05-22_05-34-21"
sensor.bc98  <- read.csv(paste("../data/", directory.name, "/sensor_BC98.csv", sep =""))
summary(sensor.bc98)

sensor.bc98.subset.1 <- sensor.bc98[1:(nrow(sensor.bc98)/2),]
sensor.bc98.subset.2 <- sensor.bc98[(nrow(sensor.bc98)/2):nrow(sensor.bc98),]

jerk.cost   <- CalculateJerkCost(sensor.bc98$Timestamp, sensor.bc98$Accelerometer.X, sensor.bc98$Accelerometer.Y, sensor.bc98$Accelerometer.Z, T)
jerk.cost.1 <- CalculateJerkCost(sensor.bc98.subset.1$Timestamp, sensor.bc98.subset.1$Accelerometer.X, sensor.bc98.subset.1$Accelerometer.Y, sensor.bc98.subset.1$Accelerometer.Z, T)
jerk.cost.2 <- CalculateJerkCost(sensor.bc98.subset.2$Timestamp, sensor.bc98.subset.2$Accelerometer.X, sensor.bc98.subset.2$Accelerometer.Y, sensor.bc98.subset.2$Accelerometer.Z, T)

directory.name              <- "2014-03-06"
motion.sensor.data.subset   <- read.csv(paste("../data/", directory.name, "/2014-03-06-t13-01-50-leg-data-subset-4.csv", sep =""))
summary(motion.sensor.data.subset)

m   <- 1
n   <- nrow(motion.sensor.data.subset)
t   <- motion.sensor.data.subset[m:n, 1] - motion.sensor.data.subset[m, 1]
gx  <- -motion.sensor.data.subset[m:n, 5]
fs  <- 1 / (length(gx) / (t[length(t)] / 1000))

indexes <- DetectMidSwing(t, gx, fs, c = 1.5, plot = T)

par(mfrow = c(3, 1))
plot(t[indexes] / 1000, c(NA, diff(t[indexes])) / 1000, type = "l", xlab = "Time (s)", ylab = "Step-to-Step Interval (s)", xaxs = "i", yaxs = "i")
(length(indexes) * 2) / ((t[indexes[length(indexes)]] - t[indexes[1]]) / 60000)

rr.data <- read.csv(paste("../data/", directory.name, "/2014-03-06-t13-01-50-rr-data-subset-4.txt", sep =""), header = F, na.strings = "", fill = T, skip = 97, col.names = c("NA1", "Time", "RRInterval", "FFTFrequency", "FFTPSD", "ARFrequency", "ARPSD", "NA2", "NA3", "NA4", "NA5"))
rr.data <- rr.data[!is.na(rr.data$RRInterval),]
rr.data$Time <- rr.data$Time - rr.data$Time[1]
plot(rr.data$Time, rr.data$RRInterval, type = "l", xlab = "Time (s)", ylab = "RR Interval (s)", xaxs = "i", yaxs = "i")
(length(rr.data$RRInterval)) / ((rr.data$Time[nrow(rr.data)] - rr.data$Time[1]) / 60)

jerk.costs <- c(0)
for(i in 1:(length(indexes) - 1)) {
  s <- motion.sensor.data.subset[indexes[i]:indexes[i + 1],]
  jerk.cost <- CalculateJerkCost(s[, 1], s[, 2], s[, 3], s[,4], F)
  jerk.costs <- c(jerk.costs, jerk.cost)
}

plot(t[indexes] / 1000, jerk.costs, type = "l", xlab = "Time (s)", ylab = "Jerk cost", xaxs = "i", yaxs = "i")
summary(jerk.costs)
