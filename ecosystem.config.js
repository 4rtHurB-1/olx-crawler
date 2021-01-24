module.exports = {
  apps : [{
    "name": "olx-crawler",
    "args": [
      "-jar",
      "bin/olx.jar"
    ],
    "script": "java",
    "max_memory_restart": "1000M",
    "node_args": [],
    "log_date_format": "YYYY-MM-DD HH:mm Z",
    "exec_interpreter": "none",
    "exec_mode": "fork",
    "log_file": "logs.log"
  }],
};
