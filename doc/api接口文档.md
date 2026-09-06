# OpenAPI definition


**简介**:OpenAPI definition


**HOST**:http://localhost:8090


**联系人**:


**Version**:v0


**接口路径**:/v3/api-docs/default


[TOC]






# 工作台-会议


## 新增会议


**接口地址**:`/meeting/addMeeting`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "meetingId": 0,
  "userId": 0,
  "title": "",
  "meetingTime": "",
  "location": "",
  "participants": "",
  "attachments": "",
  "aiSummary": "",
  "status": "",
  "remark": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|meeting|Meeting|body|true|Meeting|Meeting|
|&emsp;&emsp;meetingId|||false|integer(int64)||
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;title|||false|string||
|&emsp;&emsp;meetingTime|||false|string(date-time)||
|&emsp;&emsp;location|||false|string||
|&emsp;&emsp;participants|||false|string||
|&emsp;&emsp;attachments|||false|string||
|&emsp;&emsp;aiSummary|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 批量删除会议


**接口地址**:`/meeting/deleteBatchMeeting`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|integers|integer|body|true|array||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 获取会议分页列表


**接口地址**:`/meeting/getMeetingPage`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>仅返回当前登录用户的会议</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pageNum||query|false|integer(int32)||
|pageRows||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 删除会议材料


**接口地址**:`/meeting/removeAttachment`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 修改会议


**接口地址**:`/meeting/updateMeeting`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "meetingId": 0,
  "userId": 0,
  "title": "",
  "meetingTime": "",
  "location": "",
  "participants": "",
  "attachments": "",
  "aiSummary": "",
  "status": "",
  "remark": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|meeting|Meeting|body|true|Meeting|Meeting|
|&emsp;&emsp;meetingId|||false|integer(int64)||
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;title|||false|string||
|&emsp;&emsp;meetingTime|||false|string(date-time)||
|&emsp;&emsp;location|||false|string||
|&emsp;&emsp;participants|||false|string||
|&emsp;&emsp;attachments|||false|string||
|&emsp;&emsp;aiSummary|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 上传会议材料（阿里云 OSS）


**接口地址**:`/meeting/uploadAttachment`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|meetingId||query|true|integer(int64)||
|file||query|true|file||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# 工作台-任务


## 新增任务


**接口地址**:`/task/addTask`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "taskId": 0,
  "userId": 0,
  "nodeType": "task",
  "archived": "0",
  "parentTaskId": 0,
  "planBatchId": "",
  "title": "",
  "description": "",
  "status": "",
  "priority": 0,
  "dueTime": "",
  "tags": "",
  "displayOrder": 0,
  "remark": "",
  "attachments": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|task|Task|body|true|Task|Task|
|&emsp;&emsp;taskId|||false|integer(int64)||
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;nodeType|project / phase / task|false|string||
|&emsp;&emsp;archived|仅 project：0-否 1-是|false|string||
|&emsp;&emsp;parentTaskId|||false|integer(int64)||
|&emsp;&emsp;planBatchId|||false|string||
|&emsp;&emsp;title|||false|string||
|&emsp;&emsp;description|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;priority|||false|integer(int32)||
|&emsp;&emsp;dueTime|||false|string(date-time)||
|&emsp;&emsp;tags|||false|string||
|&emsp;&emsp;displayOrder|||false|integer(int32)||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;attachments|||false|string||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 批量删除任务


**接口地址**:`/task/deleteBatchTask`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|integers|integer|body|true|array||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 归档/恢复项目根


**接口地址**:`/task/setProjectArchived`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**接口描述**:仅 `nodeType=project` 的根任务可归档；归档后子孙附件只读。


**请求示例**:

```javascript
{
  "projectTaskId": 10,
  "archived": "1"
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 |
| -------- | -------- | ----- | -------- | -------- |
|projectTaskId|项目根任务 ID|body|true|integer(int64)|
|taskId|兼容字段，等同 projectTaskId|body|false|integer(int64)|
|archived|0-恢复，1-归档|body|true|string|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


## 获取任务分页列表


**接口地址**:`/task/getTaskPage`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>仅返回当前登录用户的任务；可按 nodeType / parentTaskId / archived 过滤</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pageNum||query|false|integer(int32)||
|pageRows||query|false|integer(int32)||
|nodeType|project / phase / task|query|false|string||
|parentTaskId|父任务 ID|query|false|integer(int64)||
|archived|仅对项目根有意义：0/1|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 项目附件审查（汇总下属任务附件）


**接口地址**:`/task/listProjectAttachments`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|projectTaskId|项目根任务 ID|query|true|integer(int64)||
|projectId|兼容旧参数，等同 projectTaskId|query|false|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 删除任务附件


**接口地址**:`/task/removeAttachment`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 更新任务附件处理状态（0-未处理，1-已处理，2-处理完成）


**接口地址**:`/task/updateAttachmentStatus`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 修改任务


**接口地址**:`/task/updateTask`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "taskId": 0,
  "userId": 0,
  "nodeType": "task",
  "archived": "0",
  "parentTaskId": 0,
  "planBatchId": "",
  "title": "",
  "description": "",
  "status": "",
  "priority": 0,
  "dueTime": "",
  "tags": "",
  "displayOrder": 0,
  "remark": "",
  "attachments": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|task|Task|body|true|Task|Task|
|&emsp;&emsp;taskId|||false|integer(int64)||
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;nodeType|project / phase / task|false|string||
|&emsp;&emsp;archived|仅 project：0-否 1-是|false|string||
|&emsp;&emsp;parentTaskId|||false|integer(int64)||
|&emsp;&emsp;planBatchId|||false|string||
|&emsp;&emsp;title|||false|string||
|&emsp;&emsp;description|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;priority|||false|integer(int32)||
|&emsp;&emsp;dueTime|||false|string(date-time)||
|&emsp;&emsp;tags|||false|string||
|&emsp;&emsp;displayOrder|||false|integer(int32)||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;attachments|||false|string||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 上传任务附件（阿里云 OSS）


**接口地址**:`/task/uploadAttachment`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||query|true|integer(int64)||
|file||query|true|file||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# 工作台-日程


## 新增日程


**接口地址**:`/event/addEvent`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "eventId": 0,
  "userId": 0,
  "taskId": 0,
  "title": "",
  "location": "",
  "startTime": "",
  "endTime": "",
  "isAllDay": 0,
  "repeatType": "",
  "repeatWeekdays": "",
  "displayOrder": 0,
  "remark": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|event|Event|body|true|Event|Event|
|&emsp;&emsp;eventId|||false|integer(int64)||
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;taskId|||false|integer(int64)||
|&emsp;&emsp;title|||false|string||
|&emsp;&emsp;location|||false|string||
|&emsp;&emsp;startTime|||false|string(date-time)||
|&emsp;&emsp;endTime|||false|string(date-time)||
|&emsp;&emsp;isAllDay|||false|integer(int32)||
|&emsp;&emsp;repeatType|||false|string||
|&emsp;&emsp;repeatWeekdays|||false|string||
|&emsp;&emsp;displayOrder|||false|integer(int32)||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 批量删除日程


**接口地址**:`/event/deleteBatchEvent`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|integers|integer|body|true|array||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 获取日程分页列表


**接口地址**:`/event/getEventPage`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>仅返回当前登录用户的日程</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pageNum||query|false|integer(int32)||
|pageRows||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 修改日程


**接口地址**:`/event/updateEvent`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "eventId": 0,
  "userId": 0,
  "taskId": 0,
  "title": "",
  "location": "",
  "startTime": "",
  "endTime": "",
  "isAllDay": 0,
  "repeatType": "",
  "repeatWeekdays": "",
  "displayOrder": 0,
  "remark": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|event|Event|body|true|Event|Event|
|&emsp;&emsp;eventId|||false|integer(int64)||
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;taskId|||false|integer(int64)||
|&emsp;&emsp;title|||false|string||
|&emsp;&emsp;location|||false|string||
|&emsp;&emsp;startTime|||false|string(date-time)||
|&emsp;&emsp;endTime|||false|string(date-time)||
|&emsp;&emsp;isAllDay|||false|integer(int32)||
|&emsp;&emsp;repeatType|||false|string||
|&emsp;&emsp;repeatWeekdays|||false|string||
|&emsp;&emsp;displayOrder|||false|integer(int32)||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# 工作台-项目


> **说明（方案 A）**：项目已并入任务树，原 `/project/*` 接口已废弃。请使用 `nodeType=project` 的 `/task/*` 管理项目根节点（以及 `phase` / `task` 子节点）。存量库请执行增量脚本 `doc/sql/migrate_project_into_task.sql`（全量 dump `remo_assistant.sql` / `remo_assistant(s).sql` 保持不变）。


# 角色管理


## 新增角色


**接口地址**:`/role/addRole`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "roleId": 0,
  "roleName": "",
  "roleCode": "",
  "level": 0,
  "description": "",
  "status": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sysRole|SysRole|body|true|SysRole|SysRole|
|&emsp;&emsp;roleId|||false|integer(int64)||
|&emsp;&emsp;roleName|||false|string||
|&emsp;&emsp;roleCode|||false|string||
|&emsp;&emsp;level|||false|integer(int32)||
|&emsp;&emsp;description|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 批量删除角色


**接口地址**:`/role/deleteBatchRole`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|integers|integer|body|true|array||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 获取角色分页列表


**接口地址**:`/role/getRolePage`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取角色分页列表</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pageNum||query|false|integer(int32)||
|pageRows||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 修改角色


**接口地址**:`/role/updateRole`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "roleId": 0,
  "roleName": "",
  "roleCode": "",
  "level": 0,
  "description": "",
  "status": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sysRole|SysRole|body|true|SysRole|SysRole|
|&emsp;&emsp;roleId|||false|integer(int64)||
|&emsp;&emsp;roleName|||false|string||
|&emsp;&emsp;roleCode|||false|string||
|&emsp;&emsp;level|||false|integer(int32)||
|&emsp;&emsp;description|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# 角色权限管理


## 给角色新增权限


**接口地址**:`/rolePermission/addRolePermission`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>为指定角色绑定一个权限</p>



**请求示例**:


```javascript
{
  "id": 0,
  "roleId": 0,
  "permissionId": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|rolePermission|RolePermission|body|true|RolePermission|RolePermission|
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;roleId|||false|integer(int64)||
|&emsp;&emsp;permissionId|||false|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 查询角色对应的权限


**接口地址**:`/rolePermission/getPermissionsByRoleId`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据角色ID查询已绑定的权限列表</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|roleId||query|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 移除角色权限


**接口地址**:`/rolePermission/removeRolePermission`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>解除角色与指定权限的绑定</p>



**请求示例**:


```javascript
{
  "id": 0,
  "roleId": 0,
  "permissionId": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|rolePermission|RolePermission|body|true|RolePermission|RolePermission|
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;roleId|||false|integer(int64)||
|&emsp;&emsp;permissionId|||false|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# 权限管理


## 新增权限


**接口地址**:`/permission/addPermission`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "permissionId": 0,
  "name": "",
  "parentId": 0,
  "type": "",
  "status": "",
  "code": "",
  "icon": "",
  "remark": "",
  "routerName": "",
  "displayOrder": 0,
  "componentPath": "",
  "isDisplay": 0,
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sysPermission|SysPermission|body|true|SysPermission|SysPermission|
|&emsp;&emsp;permissionId|||false|integer(int64)||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;parentId|||false|integer(int64)||
|&emsp;&emsp;type|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;code|||false|string||
|&emsp;&emsp;icon|||false|string||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;routerName|||false|string||
|&emsp;&emsp;displayOrder|||false|integer(int32)||
|&emsp;&emsp;componentPath|||false|string||
|&emsp;&emsp;isDisplay|||false|integer(int32)||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 批量删除权限


**接口地址**:`/permission/deleteBatchPermission`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|integers|integer|body|true|array||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 获取权限分页列表


**接口地址**:`/permission/getPermissionPage`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取权限分页列表</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pageNum||query|false|integer(int32)||
|pageRows||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 修改权限


**接口地址**:`/permission/updatePermission`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "permissionId": 0,
  "name": "",
  "parentId": 0,
  "type": "",
  "status": "",
  "code": "",
  "icon": "",
  "remark": "",
  "routerName": "",
  "displayOrder": 0,
  "componentPath": "",
  "isDisplay": 0,
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sysPermission|SysPermission|body|true|SysPermission|SysPermission|
|&emsp;&emsp;permissionId|||false|integer(int64)||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;parentId|||false|integer(int64)||
|&emsp;&emsp;type|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;code|||false|string||
|&emsp;&emsp;icon|||false|string||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;routerName|||false|string||
|&emsp;&emsp;displayOrder|||false|integer(int32)||
|&emsp;&emsp;componentPath|||false|string||
|&emsp;&emsp;isDisplay|||false|integer(int32)||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# 验证码


## 获取图形验证码


**接口地址**:`/captchas/getCaptchaBase64`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回 Base64 图片、校验 token 与时间戳；登录/注册时回传明文验证码 + token + timestamp 完成校验</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# 用户-管理员列表


## 批量删除用户


**接口地址**:`/user/deleteBatchUser`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|integers|integer|body|true|array||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 分页查询管理员列表


**接口地址**:`/user/getPageAdmin`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pageNum||query|false|integer(int32)||
|pageRows||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 分页查询普通用户列表


**接口地址**:`/user/getPageUser`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pageNum||query|false|integer(int32)||
|pageRows||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 新增用户


**接口地址**:`/user/insertUser`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "userId": 0,
  "account": "",
  "password": "",
  "status": "",
  "roleId": 0,
  "username": "",
  "phone": "",
  "email": "",
  "realName": "",
  "sex": "",
  "avatar": "",
  "birthday": "",
  "lastLoginTime": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sysUser|SysUser|body|true|SysUser|SysUser|
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;account|||false|string||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;roleId|||false|integer(int64)||
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;phone|||false|string||
|&emsp;&emsp;email|||false|string||
|&emsp;&emsp;realName|||false|string||
|&emsp;&emsp;sex|||false|string||
|&emsp;&emsp;avatar|||false|string||
|&emsp;&emsp;birthday|||false|string(date-time)||
|&emsp;&emsp;lastLoginTime|||false|string(date-time)||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 更新用户


**接口地址**:`/user/updateUser`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "userId": 0,
  "account": "",
  "password": "",
  "status": "",
  "roleId": 0,
  "username": "",
  "phone": "",
  "email": "",
  "realName": "",
  "sex": "",
  "avatar": "",
  "birthday": "",
  "lastLoginTime": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sysUser|SysUser|body|true|SysUser|SysUser|
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;account|||false|string||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;roleId|||false|integer(int64)||
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;phone|||false|string||
|&emsp;&emsp;email|||false|string||
|&emsp;&emsp;realName|||false|string||
|&emsp;&emsp;sex|||false|string||
|&emsp;&emsp;avatar|||false|string||
|&emsp;&emsp;birthday|||false|string(date-time)||
|&emsp;&emsp;lastLoginTime|||false|string(date-time)||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# 用户个人信息修改


## 修改密码


**接口地址**:`/user-profile/changePassword`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 获取当前用户信息


**接口地址**:`/user-profile/getCurrentUserProfile`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 上传用户头像


**接口地址**:`/user-profile/updateAvatar`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|file||query|true|file||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 更新个人信息


**接口地址**:`/user-profile/updateUserProfile`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "userId": 0,
  "account": "",
  "password": "",
  "status": "",
  "roleId": 0,
  "username": "",
  "phone": "",
  "email": "",
  "realName": "",
  "sex": "",
  "avatar": "",
  "birthday": "",
  "lastLoginTime": "",
  "createTime": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sysUser|SysUser|body|true|SysUser|SysUser|
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;account|||false|string||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;roleId|||false|integer(int64)||
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;phone|||false|string||
|&emsp;&emsp;email|||false|string||
|&emsp;&emsp;realName|||false|string||
|&emsp;&emsp;sex|||false|string||
|&emsp;&emsp;avatar|||false|string||
|&emsp;&emsp;birthday|||false|string(date-time)||
|&emsp;&emsp;lastLoginTime|||false|string(date-time)||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# 用户认证


## 获取认证公开配置


**接口地址**:`/auth/config`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回注册与验证码开关，供登录页按需展示</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 用户登录


**接口地址**:`/auth/login`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>使用账号、手机号或邮箱登录，需要提供密码；验证码按配置可选</p>



**请求示例**:


```javascript
{
  "account": "",
  "password": "",
  "captchaCode": "",
  "captchaToken": "",
  "captchaTimestamp": 0,
  "token": "",
  "userId": 0,
  "username": "",
  "avatar": "",
  "menuList": [
    {
      "permissionId": 0,
      "name": "",
      "parentId": 0,
      "type": "",
      "status": "",
      "code": "",
      "icon": "",
      "remark": "",
      "routerName": "",
      "displayOrder": 0,
      "componentPath": "",
      "isDisplay": 0,
      "createTime": "",
      "updateTime": ""
    }
  ]
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|login|Login|body|true|Login|Login|
|&emsp;&emsp;account|||false|string||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;captchaCode|||false|string||
|&emsp;&emsp;captchaToken|||false|string||
|&emsp;&emsp;captchaTimestamp|||false|integer(int64)||
|&emsp;&emsp;token|||false|string||
|&emsp;&emsp;userId|||false|integer(int64)||
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;avatar|||false|string||
|&emsp;&emsp;menuList|||false|array|SysPermission|
|&emsp;&emsp;&emsp;&emsp;permissionId|||false|integer(int64)||
|&emsp;&emsp;&emsp;&emsp;name|||false|string||
|&emsp;&emsp;&emsp;&emsp;parentId|||false|integer(int64)||
|&emsp;&emsp;&emsp;&emsp;type|||false|string||
|&emsp;&emsp;&emsp;&emsp;status|||false|string||
|&emsp;&emsp;&emsp;&emsp;code|||false|string||
|&emsp;&emsp;&emsp;&emsp;icon|||false|string||
|&emsp;&emsp;&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;&emsp;&emsp;routerName|||false|string||
|&emsp;&emsp;&emsp;&emsp;displayOrder|||false|integer(int32)||
|&emsp;&emsp;&emsp;&emsp;componentPath|||false|string||
|&emsp;&emsp;&emsp;&emsp;isDisplay|||false|integer(int32)||
|&emsp;&emsp;&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 获取当前用户菜单


**接口地址**:`/auth/menus`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>从数据库实时加载当前登录用户可访问的菜单与功能权限</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 用户注册


**接口地址**:`/auth/register`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>通过手机号或邮箱进行注册，需要提供密码、确认密码；验证码按配置可选</p>



**请求示例**:


```javascript
{
  "phone": "",
  "email": "",
  "password": "",
  "confirmPassword": "",
  "captchaCode": "",
  "captchaToken": "",
  "captchaTimestamp": 0,
  "account": "",
  "userId": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|register|Register|body|true|Register|Register|
|&emsp;&emsp;phone|||false|string||
|&emsp;&emsp;email|||false|string||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;confirmPassword|||false|string||
|&emsp;&emsp;captchaCode|||false|string||
|&emsp;&emsp;captchaToken|||false|string||
|&emsp;&emsp;captchaTimestamp|||false|integer(int64)||
|&emsp;&emsp;account|||false|string||
|&emsp;&emsp;userId|||false|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


# Ai对话


## Ai对话（流式）


**接口地址**:`/ai/chat`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`text/event-stream`


**接口描述**:<p>发送消息并接收流式响应</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|message|用户消息内容|query|true|string||
|conversationId|会话ID，不传则创建新会话|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## 删除会话


**接口地址**:`/ai/deleteConversation`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|conversationId|会话ID|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 获取历史消息


**接口地址**:`/ai/getHistory`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|conversationId|会话ID|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## 获取会话列表


**接口地址**:`/ai/listConversations`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## AI 会议整理（根据材料生成概要并写回）


**接口地址**:`/ai/meeting/summary`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## AI 规划确认落板


**接口地址**:`/ai/plan/apply`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>将预览阶段写入任务树。body 使用 <code>projectTaskId</code>（项目根任务 ID）；后端仍兼容旧字段 <code>projectId</code>。</p>



**请求示例**:


```javascript
{
  "projectTaskId": 0,
  "planBatchId": "",
  "phases": []
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|body|Map|body|true|object||
|&emsp;&emsp;projectTaskId|项目根任务 ID（nodeType=project）|body|true|integer(int64)||
|&emsp;&emsp;projectId|兼容旧字段，等同 projectTaskId|body|false|integer(int64)||
|&emsp;&emsp;planBatchId|规划批次号|body|true|string||
|&emsp;&emsp;phases|阶段与步骤|body|true|array||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```


## AI 规划预览（不写库）


**接口地址**:`/ai/plan/preview`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>按项目根任务生成阶段规划预览（不写库）。body 使用 <code>projectTaskId</code>；后端仍兼容旧字段 <code>projectId</code>。</p>



**请求示例**:


```javascript
{
  "projectTaskId": 0,
  "deadline": "2026-10-01",
  "goal": "",
  "constraints": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|body|Map|body|true|object||
|&emsp;&emsp;projectTaskId|项目根任务 ID（nodeType=project）|body|true|integer(int64)||
|&emsp;&emsp;projectId|兼容旧字段，等同 projectTaskId|body|false|integer(int64)||
|&emsp;&emsp;deadline|项目截止时间|body|true|string||
|&emsp;&emsp;goal|规划目标（可选，默认取项目描述）|body|false|string||
|&emsp;&emsp;constraints|约束说明|body|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|msg||string||
|data||string||


**响应示例**:
```javascript
{
	"code": 0,
	"msg": "",
	"data": {}
}
```