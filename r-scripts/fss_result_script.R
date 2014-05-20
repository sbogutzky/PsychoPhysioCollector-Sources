setwd("~/Entwicklung/projects/bogutzky/repositories/data-collector-android/r-scripts")
source("/Users/simonbogutzky/Entwicklung/projects/bogutzky/repositories/flow-gait-analysis/r-code/scripts/final/functions/fss-functions.R")

directory.name <- "2014-05-19_17-12-43"

scale <- read.csv(paste("../data/", directory.name, "/scale.csv", sep =""))
results <- CalculateFlowShortScaleFactors(cbind(scale[3:18], scale$System.Timestamp.01, scale$System.Timestamp.02))
summary(results)

sensor.bd38 <- read.csv(paste("../data/", directory.name, "/sensor_BD38.csv", sep =""))

for(i in 1:nrow(scale)) {
  if(i == 1) {
    print(paste("1. ", "Start to", as.POSIXct(scale$System.Timestamp.01[i] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")))
    subset <- sensor.bd38[sensor.bd38$System.Timestamp < scale$System.Timestamp.01[i],]
    print(paste("2. ", as.POSIXct(subset$System.Timestamp[1] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"), "to", as.POSIXct(subset$System.Timestamp[nrow(subset)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"))) 
  } else {
    print(paste("1. ", as.POSIXct(scale$System.Timestamp.02[i - 1] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"), "to", as.POSIXct(scale$System.Timestamp.01[i] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")))
    subset <- sensor.bd38[sensor.bd38$System.Timestamp >= scale$System.Timestamp.02[i - 1] & sensor.bd38$System.Timestamp < scale$System.Timestamp.01[i],]
    print(paste("2. ", as.POSIXct(subset$System.Timestamp[1] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"), "to", as.POSIXct(subset$System.Timestamp[nrow(subset)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"))) 
  }
  write.csv(subset, file = paste("../data/", directory.name, "/sensor_BD38", "_subset_", i, ".csv", sep =""), row.names = FALSE)
  print (paste("Wrote: ", "./data/", directory.name, "/sensor_BD38", "_subset_", i, ".csv", sep =""))
}

