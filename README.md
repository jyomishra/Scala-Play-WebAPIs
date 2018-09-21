# Scala-Play-BitCoin-Example

## Running

Run this using [sbt](http://www.scala-sbt.org/).  

```bash
sbt run
```

And then go to <http://localhost:9000> to see the running web application.

## Supported APIs

#### 1. BitCoin History : /bitCoin/history/?period={period}

##### By Week : 
    {{host}}/bitCoin/history/?period=week

##### By Month : 
    {{host}}/bitCoin/history/?period=month

##### By Custom Date : 
    {{host}}/bitCoin/history/?startDate={startDate}&endDate={endDate}
        
    Example: startDate = 2018-02-10 (yyyy-mm-dd) endDate = 2018-02-20

#### 2. BitCoin Prediction with Days
    {{host}}/bitCoin/prediction/?period={days}

#### 3. BitCoin Rolling Average Between Custom Dates
    {{host}}/bitCoin/rollingAvg/?startDate={startDate}&endDate={endDate}
                             
    Example: startDate = 2018-02-10 (yyyy-mm-dd) endDate = 2018-02-20
