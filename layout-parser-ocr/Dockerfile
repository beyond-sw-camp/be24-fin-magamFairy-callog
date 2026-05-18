FROM pytorch/pytorch:1.10.0-cuda11.3-cudnn8-runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    LAYOUT_MODEL_DEVICE=cpu

WORKDIR /service

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential \
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

COPY app ./app

EXPOSE 8001

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
