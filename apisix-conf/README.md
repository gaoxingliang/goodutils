# What it is?
a cli to configure apisix upstream / routes by the [apisix admin api](https://apisix.apache.org/docs/apisix/admin-api/#upstream-api).
# build
``
./gradlew customFatJar
``

# support configure apisix in cli.
"-u" is the url of admin api. "-k" is the api key.<br>
an example to update all `upstreams` timeout settings:
```sh
java -jar apisixconf.jar upstreams update '{"timeout":{"connect": 10,"send": 10,"read": 30}}' -u http://127.0.0.1:9180 -k edd1c9f034335f136f87ad84b625c8f1 -all
```
update all `routes`:
```sh
java -jar apisixconf.jar routes update '{"timeout":{"connect": 10,"send": 10,"read": 30}}' -u http://127.0.0.1:9180 -k edd1c9f034335f136f87ad84b625c8f1 -all
```

list all routes:
```sh
java -jar apisixconf.jar routes list -u http://127.0.0.1:9180 -k edd1c9f034335f136f87ad84b625c8f1
```
<br>
