Overview
--------

TODO

Running the minimal example
---------------------------

### Step 1: Setup the message queues

```
root_dir=/tmp/mqueue
mkdir $root_dir
sudo mount -t mqueue none $root_dir
touch $root_dir/rishin_in
touch $root_dir/rishin_out
```

### Step 2: Run the echo mechanism for the message loops

See the tools in algowebsolve-processor project.


```
./mqreader -q /rishin_out | ./pipedelay -d 900 | tee /dev/pts/1 | ./mqwriter -q /rishin_in

```

### Step 3: Run webapp and curl

```
mvn spring-boot:run
curl -X PUT \
    -H "Content-Type: application/json" \
    -d '{"type":"dynamic-backpack1d","details":{"maxweight":1}}' \
    http://localhost:8080/v1/problems
```



