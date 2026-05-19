FROM pytorch/pytorch:1.10.0-cuda11.3-cudnn8-runtime

ARG LAYOUT_MODEL_CONFIG_URL=https://huggingface.co/layoutparser/detectron2/resolve/main/PubLayNet/faster_rcnn_R_50_FPN_3x/config.yml
ARG LAYOUT_MODEL_WEIGHTS_URL=https://huggingface.co/layoutparser/detectron2/resolve/main/PubLayNet/faster_rcnn_R_50_FPN_3x/model_final.pth

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    LAYOUT_MODEL_CONFIG=/opt/models/layout/config.yml \
    LAYOUT_MODEL_WEIGHTS=/opt/models/layout/model_final.pth \
    LAYOUT_MODEL_DEVICE=cpu \
    PADDLE_OCR_BASE_DIR=/opt/models/paddleocr \
    PADDLEOCR_HOME=/opt/models/paddleocr \
    LOAD_MODEL_ON_STARTUP=true \
    OCR_LOAD_MODEL_ON_STARTUP=true

WORKDIR /service

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        git \
        libglib2.0-0 \
        libgl1 \
        libsm6 \
        libxext6 \
        libxrender1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
# PaddleOCR 2.7.3 has the packaged tools module needed by the runtime, but its
# dependency metadata requires Pillow 10+, which is unavailable on Python 3.7.
RUN python -m pip install --upgrade pip \
    && python -m pip install -r requirements.txt \
        -f https://www.paddlepaddle.org.cn/whl/linux/mkl/avx/stable.html \
    && python -m pip install --no-deps paddleocr==2.7.3 \
    && python -m pip install detectron2 \
        -f https://dl.fbaipublicfiles.com/detectron2/wheels/cu113/torch1.10/index.html

RUN mkdir -p /opt/models/layout /opt/models/paddleocr \
    && python -c "from urllib.request import urlretrieve; urlretrieve('${LAYOUT_MODEL_CONFIG_URL}', '/opt/models/layout/config.yml'); urlretrieve('${LAYOUT_MODEL_WEIGHTS_URL}', '/opt/models/layout/model_final.pth')" \
    && python -c "from pathlib import Path; import paddleocr.paddleocr as paddleocr_module; paddleocr_module.BASE_DIR='/opt/models/paddleocr'; Path(paddleocr_module.BASE_DIR).mkdir(parents=True, exist_ok=True); from paddleocr import PaddleOCR; PaddleOCR(use_angle_cls=False, lang='korean', use_gpu=False, enable_mkldnn=False, cpu_threads=1, det_limit_side_len=1600, use_space_char=True, show_log=False)"

COPY app ./app

EXPOSE 8001

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
