#
# Visualizes TO and HS detection out of measured sensor data
# in form of motion-data.csv and motion-event-data.csv
#
# Autor: Philipp Marsch, pmarsch@stud.hs-bremen.de
#

###### Set Directory and Filenames
setwd("~/Desktop/gait test 06.09.2014")
data.name <- "motion-data"
event.name <- "motion-event-data"
name <- "philipp"
require("stringr")

# dates <- c("2014-09-06-t14-18-32", 
#            "2014-09-06-t14-19-13", 
#            "2014-09-06-t14-21-03",
#            "2014-09-06-t14-30-49")

####### TODO read dates out of filenames
# Problem: Not date can differ in one minute due to saving duration
filenames <- list.files(pattern="*.csv", full.names=TRUE)
dates <- str_extract(filenames, "[0-9]{4}-[0-9]{2}-[0-9]{2}-t[0-9]{2}-[0-9]{2}-[0-9]{2}")
dates <- dates[!duplicated(dates)]

###### Print Graphs
for (date in dates) {
  
  file.name <- paste(date, "-", data.name, "-", name, ".csv", sep="")
  if (file.exists(file.name)) {
    leg.data <- read.csv(file.name)  
  }
  
  file.name <- paste(date, "-", event.name, "-", name, ".csv", sep="")
  if (file.exists(file.name)) {
    event.data <- read.csv(file.name)  
  }
  
  # HS
  hs <- which(event.data$Name == "HS")
  to <- which(event.data$Name == "TO")
  
  # HS
  plot(leg.data$Timestamp, leg.data$ControlValueC, type = "l", xlab = "t(ms)", xaxs='i', main = paste("HS", date))
  points(event.data$Timestamp[hs], event.data$Value[hs], col="red")
  # TO
  plot(leg.data$Timestamp, leg.data$ControlValueA, type = "l", xlab = "t(ms)", xaxs='i', main = paste("TO", date))
  points(event.data$Timestamp[to], event.data$Value[to], col="blue")
  
}