a <- 1
b <- a
c <- .5
d <- .1

f1 <- 10
f2 <- 70
f3 <- 5
fs <- 800

i <- seq(0, fs, by = .1) 
x <- a * sin(i * 2*pi*f1/fs) + b* sin(i * 2*pi*f2/fs) + c* sin(i * 2*pi*f3/fs+ d * runif(1, min = 0, max = 1))

rm(i)
rm(a)
rm(b)
rm(c)
rm(d)
rm(f1)
rm(f2)
rm(f3)
rm(fs)

y <- ecg$ECG.RA.LL
t <- ecg$Timestamp
plot(t[1:1000], y[1:1000], type = "l")
p <- detectPeaks(y, 8000)
points(t[1:1000][p - 2], y[1:1000][p - 2], col = "red")

rr.data <- read.csv(paste("/Users/simonbogutzky/Entwicklung/projects/bogutzky/repositories/data-collector-android/data/2014-03-04_12-18-13", "/sensor_BD38_subset_3_hrv.txt", sep =""), header = F, na.strings = "", fill = T, skip = 97, col.names = c("NA1", "Time", "RRInterval", "FFTFrequency", "FFTPSD", "ARFrequency", "ARPSD", "NA2", "NA3", "NA4", "NA5"))
rr.data <- rr.data[!is.na(rr.data$RRInterval),]

plot(rr.data$RRInterval, type = "l")
lines(diff(p / 256), col = "red")


ti <- t[p]

ti <- ti / 1000
dfi <- diff(ti)
ti <- ti[2:length(ti)]
mean(dfi)

plot(ti, dfi)

60 / mean(dfi)

idfi <- Interpolate(ti, dfi, 2, "linear")
plot(idfi$x, idfi$y)


# Detrending
m <- lm(idfi$y ~ seq(idfi$y))
didfi <- m$residuals

plot(idfi$x, didfi)

# Hamming
didfih <- didfi * hanning(length(didfi))

plot(idfi$x, didfih)
require(TSA)
periodogram(didfih)


Interpolate <- function(x, y, interpolation.rate, method = c("linear", "nearest", "pchip", "cubic", "spline")) {
  
  # Caluculate xi
  xi <- seq(x[1], x[length(x)], by = 1/interpolation.rate)
  
  # Interpolate
  require(signal)
  yi <- interp1(x, y, xi, method = method)
  return(data.frame(x = xi, y = yi))
}


detectPeaks <- function(z, num) {
  loops <- ceiling(length(z) / num)
  loop.seq <- seq(1, loops, by = 0.5)
  loop <- 0
  peaks <- c()
  for(g in loop.seq) {
    
    stt <- ((g-1) * num) + 1
    stp <- stt + num
    x <- z[stt:stp]
    x <- x[complete.cases(x)]
    
    if (length(x) > 2) {
  
  # Detrending
  m <- lm(x ~ seq(x))
  x.detrended <- m$residuals
  
  N <- length(x)
  L <- ceiling(N / 2) - 1
  a <- 1
  
  M <- matrix(runif(L * N, min = 0, max = 1), nrow = L, ncol = N)
  for(k in 1:L) {
    
    I <- (k + 2):(N - k + 1)
    for(i in I) {
      if(x.detrended[i - 1] > x.detrended[i - k - 1] & x.detrended[i - 1] > x.detrended[i + k - 1])
        M[k, i] <- 0
    }
  }
  
  j <- rowSums(M)
  r <- which.min(j)
  
  m <- ((r + 1) * -1)
  n <- nrow(M) * -1
  
  Mr <- M[m:n,]
  
  sdCol <- apply(Mr, 2, sd)
  
  p <- which(sdCol == 0)
  p <- p + stt 
  peaks <- c(peaks, p)
    }
  loop <- loop + 1
  
  print(paste(round((loop / length(loop.seq)) * 100), "%"))
  }
  peaks <- peaks[!duplicated(peaks)]
  peaks <- sort(peaks)
  peaks <- peaks[diff(peaks) > 1]
  return(peaks)
}
