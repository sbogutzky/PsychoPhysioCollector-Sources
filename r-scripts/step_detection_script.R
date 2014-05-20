setwd("~/Entwicklung/projects/bogutzky/repositories/data-collector-android/r-scripts")

source("/Users/simonbogutzky/Entwicklung/projects/hochschule-bremen/repositories/gait-analysis/gang-ereignis-erkennung/r-code/finale-scripte/filter.R")
#source("studium/hiwi/repo/gang-ereignis-erkennung/r-code/finale-scripte/searchLows.R")
#source("studium/hiwi/repo/gang-ereignis-erkennung/r-code/finale-scripte/searchHighs.R")
#source("studium/hiwi/repo/gang-ereignis-erkennung/r-code/roh-scripte/GaitFrequency.R")

directory.name <- "2014-05-17_17-25-09"

sensor.bc98.subset <- read.csv(paste("../data/", directory.name, "/sensor_BC98_subset_3.csv", sep =""))
summary(sensor.bc98.subset)
plot(sensor.bc98.subset$Accelerometer.X[2000:3000], type = "l")


CalculateJerk <- function(t, x) {
  total.jerk <- c()
  for(i in 1:length(t)) {
    if(i < 2) {
      total.jerk <- c(total.jerk, 0)
    } else {
      jerk <- (x[i] - x[i - 1]) / (t[i] - t[i - 1])
      total.jerk <- c(total.jerk, jerk)
    }
  }
  return(total.jerk)
}

jerk.x <- CalculateJerk(sensor.bc98.subset$Timestamp[2000:3000], sensor.bc98.subset$Accelerometer.X[2000:3000])

jerk.y <- CalculateJerk(sensor.bc98.subset$Timestamp[2000:3000], sensor.bc98.subset$Accelerometer.Y[2000:3000])

jerk.z <- CalculateJerk(sensor.bc98.subset$Timestamp[2000:3000], sensor.bc98.subset$Accelerometer.Z[2000:3000])

CalculateJerkCost(jerk.x, jerk.y, jerk.z)


signal.lowpass <- IIRLowPass1stOrder(sensor.bc98.subset$Gyroscope.X[2000:3000], 0.5)

plot(signal.lowpass, type = "l")



CalculateCadence <- function(x) {
  
  signal.lowpass <- IIRLowPass1stOrder(x, 0.01)
  
  lows <- searchLows(signal.lowpass)
  highs <- searchHighs(signal.lowpass)
  
  # extract extremes into vectors
  highPoints <- c()
  lowPoints <- c()
  for (i in (1:length(lows))) {
    if (!is.na(lows[i])) {
      lowPoints <- c(lowPoints, lows[i])
    }
    if (!is.na(highs[i])) {
      highPoints <- c(highPoints, highs[i])
    }         
  }
  # calculate differences
  if (length(highPoints) == length(lowPoints)) {
    difference <- abs(highPoints-lowPoints) 
  } else if (length(highPoints)+1 == length(lowPoints)) {
    highPoints <- highPoints[1:length(highPoints)-1]    
  } else if (length(highPoints) == length(lowPoints)+1){
    lowPoints <- lowPoints[1:length(lowPoints)-1]
  } else {
    print("Mehr als 1 Extremum Unterschied")
  }
  
  # remove hits with less than minimum difference
  for (i in (1:length(difference))) {
    if (difference[i] < 30) {
      highPoints[i] <- NA
      lowPoints[i] <- NA
    }
  }
  # remove NA from vector lows
  j <- 1
  for (i in (1:length(lows))) {
    if (!is.na(lows[i])) {
      lows[i] <- lowPoints[j] 
      j <- j + 1
    }
  }
  
  # threshhold cleaning
  threshhold <-   quantile.upper <- quantile(signal.lowpass, 0.4)
  for (i in seq(along=lows)) {
    if ((signal.lowpass[i]>threshhold)) {
      lows[i] <- NA
    }  
  }
  MeanGaitFrequency <- GaitFrequency(leg.data$SensorTime[n:m], lows[n:m])
  inSeconds <- MeanGaitFrequency / 100 / 60
  
  allLows <- c()
  for (i in (1:(length(lows)))) {
    if (!is.na(lows[i])) {
      allLows <- c(allLows, lows[i])    
    }
  }
  
  # only do the rest if lows are found
  if(!all(is.na(lows[1:length(lows)])) && length(allLows) > 1) {
    
    #calculate intervals
    intervals <- c()
    j <- 0
    for (i in (1:(length(leg.data$SensorTime)))) {
      if (!is.na(lows[i])) {
        if (j!=0) {
          intervals <- c(intervals, abs((leg.data$SensorTime[i] - leg.data$SensorTime[j])))    
        }
        j <- i
      }
    }
    
    # write into vektor conform to others
    # intervalPoints is a vektor of equal length to the data set but contains values where TRUE in MS (the true invervals)
    intervalPoints <- c()
    j <- 1
    for (i in 1:length(lows)) {
      if (is.na(lows[i])) {
        intervalPoints <- c(intervalPoints, NA)    
      } else {
        intervalPoints <- c(intervalPoints, intervals[j])
        j <- j + 1
      }
    }
    
    
    # round to whole ms
    intervals <- round(intervals, digits=0)
    
    # intervalAxis contains SensorTimes - used as x-axis
    intervalAxis <- c()
    for(i in 1:nrow(leg.data)) {
      if (!is.na(intervalPoints[i])) {
        intervalAxis <- c(intervalAxis, leg.data$SensorTime[i])
      }
    }
    
    if (length(intervalAxis) == length(intervals)){   
      pdf(paste(data.file.path, file.name.date.prefix, filename.ending, "-gait-interval.pdf", sep = ""))
      plot(intervalAxis, intervals, type="l", xlab="Sensor time in ms", ylab="Gait interval in ms", main=paste(file.name.date.prefix, filename.ending))
      dev.off()
      print(paste("PDF erstellt: ", file.name.date.prefix, filename.ending), sep = "")
    } else {
      print(paste("Unterschiedliche Laenge: ", file.name.data.prefix, filename.ending), sep = "")
    }
    
    
    # write to file
    if (length(intervals)>=2) {
      write.csv(file=paste(data.file.path, file.name.date.prefix, filename.ending, "-gait-interval.csv", sep = ""), intervals, row.names=FALSE)
      print(paste("CSV erstellt: ", file.name.date.prefix, filename.ending), sep = "")
    } else {
      print(paste("Nicht genug Intervalle berechnet: ", file.name.date.prefix, filename.ending), sep = "")
    }
    
  } else {
    print(paste("Nur 2 oder weniger Tiefpunkte gefunden: ", file.name.date.prefix, filename.ending), sep = "")
  }
}