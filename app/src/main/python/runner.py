import sys, io, traceback


def execute(code, filename="<editor>"):
    """Ejecuta código Python y devuelve todo lo impreso por stdout/stderr."""
    buf = io.StringIO()
    old_out = sys.stdout
    sys.stdout = buf
    try:
        compiled = compile(code, filename, "exec")
        exec(compiled, {"__name__": "__main__"})
    except Exception:
        traceback.print_exc(file=buf)
    finally:
        sys.stdout = old_out
    return buf.getvalue()
