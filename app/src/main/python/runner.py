import sys
import io
import traceback


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


class _KeyidePause(Exception):
    def __init__(self, line):
        self.line = line


def execute_debug(code, breakpoints=None, trace=False, filename="<editor>"):
    """Ejecuta con DEPURADOR: puntos de parada (para la ejecución) y traza."""
    bps = set(int(x) for x in (breakpoints or []))
    buf = io.StringIO()
    old_out = sys.stdout
    sys.stdout = buf

    def tracer(frame, event, arg):
        if event == "line" and frame.f_code.co_filename == filename:
            ln = frame.f_lineno
            if ln in bps:
                raise _KeyidePause(ln)
            if trace:
                buf.write("\u2192 L%d\n" % ln)
        return tracer

    try:
        compiled = compile(code, filename, "exec")
        sys.settrace(tracer)
        exec(compiled, {"__name__": "__main__"})
    except _KeyidePause as p:
        buf.write("\u23F8 pausa en la l\u00ednea %d (quita el punto y vuelve a ejecutar)\n" % p.line)
    except Exception:
        traceback.print_exc(file=buf)
    finally:
        sys.settrace(None)
        sys.stdout = old_out
    return buf.getvalue()
