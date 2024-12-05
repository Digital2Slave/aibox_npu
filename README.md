# aibox_npu

Develop a **cv_npu** android library which use **yolo11n.rknn** model to do object detect, and run the demo on **3588** android board.

## 1. convert yolo11 to rknn

### 1.0 mkvirtualenv yolo11
```bash
$ pip3 install virtualenv virtualenvwrapper
$ source /usr/local/bin/virtualenvwrapper.sh
$ mkvirtualenv yolo11 -p /usr/bin/python3.8
(yolo11) $ pip3 install torch==1.12.1+cpu torchvision==0.13.1+cpu torchaudio==0.12.1 --extra-index-url https://download.pytorch.org/whl/cpu
(yolo11) $ pip3 install ultralytics==8.3.38 tensorboard==2.8.0 
(yolo11) $ pip3 install onnx==1.14.1 onnxruntime==1.16.0 onnx-simplifier==0.4.8 onnxoptimizer==0.2.7 onnxsim==0.4.33
```

### 1.1 convert pt to onnx

- Refer https://github.com/airockchip/ultralytics_yolo11/blob/main/RKOPT_README.md

```bash
(yolo11) $ mkdir -p yolo11_rknn/model/
(yolo11) $ cd yolo11_rknn/model/
(yolo11) $ wget https://github.com/ultralytics/assets/releases/download/v8.3.0/yolo11n.pt
(yolo11) $ cd ~/yolo11_rknn
(yolo11) $ git clone https://github.com/airockchip/ultralytics_yolo11.git
(yolo11) $ cd ultralytics_yolo11
(yolo11) $ export PYTHONPATH=./
(yolo11) $ vi ./ultralytics/cfg/default.yaml
model: /home/tianzx/yolo11_rknn/model/yolo11n.pt 
(yolo11) $ python ./ultralytics/engine/exporter.py
Ultralytics 8.3.9 🚀 Python-3.8.10 torch-1.12.1+cpu CPU (AMD Ryzen 7 7735HS with Radeon Graphics)
YOLO11n summary (fused): 238 layers, 2,616,248 parameters, 0 gradients, 6.5 GFLOPs

PyTorch: starting from '/home/tianzx/yolo11_rknn/model/yolo11n.pt' with input shape (16, 3, 640, 640) BCHW and output shape(s) ((16, 64, 80, 80), (16, 80, 80, 80), (16, 1, 80, 80), (16, 64, 40, 40), (16, 80, 40, 40), (16, 1, 40, 40), (16, 64, 20, 20), (16, 80, 20, 20), (16, 1, 20, 20)) (5.4 MB)

RKNN: starting export with torch 1.12.1+cpu...

RKNN: feed /home/tianzx/yolo11_rknn/model/yolo11n.onnx to RKNN-Toolkit or RKNN-Toolkit2 to generate RKNN model.
Refer https://github.com/airockchip/rknn_model_zoo/tree/main/examples/
RKNN: export success ✅ 1.6s, saved as '/home/tianzx/yolo11_rknn/model/yolo11n.onnx' (10.0 MB)

Export complete (3.1s)
Results saved to /home/tianzx/yolo11_rknn/model
Predict:         yolo predict task=detect model=/home/tianzx/yolo11_rknn/model/yolo11n.onnx imgsz=640  
Validate:        yolo val task=detect model=/home/tianzx/yolo11_rknn/model/yolo11n.onnx imgsz=640 data=/usr/src/ultralytics/ultralytics/cfg/datasets/coco.yaml  
Visualize:       https://netron.app
(yolo11) $ ls -hl ../model                                
total 16M
-rw-rw-r-- 1 tianzx tianzx  11M 12月  5 09:58 yolo11n.onnx
-rw-rw-r-- 1 tianzx tianzx 5.4M 12月  5 09:57 yolo11n.pt
```

### 1.2 convert onnx to rknn

- install rknn-toolkit2

```bash
(yolo11) $ cd ~/yolo11_rknn
#!< 安装依赖库
(yolo11) $ wget https://github.com/airockchip/rknn-toolkit2/archive/refs/tags/v2.3.0.tar.gz
(yolo11) $ tar -zxf v2.3.0.tar.gz && cd rknn-toolkit2-2.3.0/rknn-toolkit2
(yolo11) $ pip install -r packages/x86_64/requirements_cp38-2.3.0.txt
(yolo11) $ pip install packages/x86_64/rknn_toolkit2-2.3.0-cp38-cp38-manylinux_2_17_x86_64.manylinux2014_x86_64.whl
(yolo11) $ rm v2.3.0.tar.gz
```

- convert onnx to rknn

```bash
(yolo11) $ cd ~/yolo11_rknn/
(yolo11) $ wget https://github.com/airockchip/rknn_model_zoo/archive/refs/tags/v2.3.0.tar.gz
(yolo11) $ tar -zxf v2.3.0.tar.gz && cd rknn_model_zoo-2.3.0/
(yolo11) $ cd examples/yolo11/python/
(yolo11) $ python convert.py /home/tianzx/yolo11_rknn/model/yolo11n.onnx rk3588 i8 /home/tianzx/yolo11_rknn/model/yolo11n.rknn
I rknn-toolkit2 version: 2.3.0
--> Config model
done
--> Loading model
I Loading : 100%|██████████████████████████████████████████████| 179/179 [00:00<00:00, 66623.52it/s]
done
--> Building model
I OpFusing 0: 100%|██████████████████████████████████████████████| 100/100 [00:00<00:00, 931.32it/s]
I OpFusing 1 : 100%|█████████████████████████████████████████████| 100/100 [00:00<00:00, 441.80it/s]
I OpFusing 0 : 100%|█████████████████████████████████████████████| 100/100 [00:00<00:00, 202.10it/s]
I OpFusing 1 : 100%|█████████████████████████████████████████████| 100/100 [00:00<00:00, 194.11it/s]
I OpFusing 2 : 100%|█████████████████████████████████████████████| 100/100 [00:00<00:00, 189.89it/s]
I OpFusing 0 : 100%|█████████████████████████████████████████████| 100/100 [00:00<00:00, 154.25it/s]
I OpFusing 1 : 100%|█████████████████████████████████████████████| 100/100 [00:00<00:00, 149.53it/s]
I OpFusing 2 : 100%|█████████████████████████████████████████████| 100/100 [00:00<00:00, 101.05it/s]
W build: found outlier value, this may affect quantization accuracy
                       const name                          abs_mean    abs_std     outlier value
                       model.23.cv3.1.1.1.conv.weight      0.55        0.60        -12.173     
                       model.23.cv3.0.0.0.conv.weight      0.25        0.35        -15.593     
I GraphPreparing : 100%|███████████████████████████████████████| 223/223 [00:00<00:00, 13654.85it/s]
I Quantizating : 100%|████████████████████████████████████████████| 223/223 [00:04<00:00, 52.12it/s]
W build: The default input dtype of 'images' is changed from 'float32' to 'int8' in rknn model for performance!
                      Please take care of this change when deploy rknn model with Runtime API!
W build: The default output dtype of '460' is changed from 'float32' to 'int8' in rknn model for performance!
                     Please take care of this change when deploy rknn model with Runtime API!
W build: The default output dtype of 'onnx::ReduceSum_474' is changed from 'float32' to 'int8' in rknn model for performance!
                     Please take care of this change when deploy rknn model with Runtime API!
W build: The default output dtype of '480' is changed from 'float32' to 'int8' in rknn model for performance!
                     Please take care of this change when deploy rknn model with Runtime API!
W build: The default output dtype of '487' is changed from 'float32' to 'int8' in rknn model for performance!
                     Please take care of this change when deploy rknn model with Runtime API!
W build: The default output dtype of 'onnx::ReduceSum_501' is changed from 'float32' to 'int8' in rknn model for performance!
                     Please take care of this change when deploy rknn model with Runtime API!
W build: The default output dtype of '507' is changed from 'float32' to 'int8' in rknn model for performance!
                     Please take care of this change when deploy rknn model with Runtime API!
W build: The default output dtype of '514' is changed from 'float32' to 'int8' in rknn model for performance!
                     Please take care of this change when deploy rknn model with Runtime API!
W build: The default output dtype of 'onnx::ReduceSum_528' is changed from 'float32' to 'int8' in rknn model for performance!
                     Please take care of this change when deploy rknn model with Runtime API!
W build: The default output dtype of '534' is changed from 'float32' to 'int8' in rknn model for performance!
                     Please take care of this change when deploy rknn model with Runtime API!
I rknn building ...
I rknn building done.
done
--> Export rknn model
done
(yolo11) $ ls -hl ~/yolo11_rknn/model
total 20M
-rw-rw-r-- 1 tianzx tianzx  11M 12月  5 09:58 yolo11n.onnx
-rw-rw-r-- 1 tianzx tianzx 5.4M 12月  5 09:57 yolo11n.pt
-rw-rw-r-- 1 tianzx tianzx 4.1M 12月  5 10:18 yolo11n.rknn
```


## 2. Test

 <img src="./docs/bus.jpg">
 
 <img src="./docs/out_20241205-105940742.jpg">
