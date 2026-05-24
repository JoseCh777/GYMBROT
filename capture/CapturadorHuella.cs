using System;
using System.IO;
using System.Windows.Forms;

namespace CapturadorHuella
{
    static class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new CapturaForm());
        }
    }

    class CapturaForm : Form, DPFP.Capture.EventHandler
    {
        private DPFP.Capture.Capture capturer;
        private DPFP.Processing.Enrollment enroller;
        private int totalNeeded = 4;

        public CapturaForm()
        {
            WindowState = FormWindowState.Normal;
            ShowInTaskbar = false;
            StartPosition = FormStartPosition.Manual;
            Location = new System.Drawing.Point(-32000, -32000);
            Width = 1;
            Height = 1;
            FormBorderStyle = FormBorderStyle.None;
            Load += CapturaForm_Load;
        }

        private void CapturaForm_Load(object sender, EventArgs e)
        {
            try
            {
                capturer = new DPFP.Capture.Capture();
                capturer.EventHandler = this;
            }
            catch (Exception ex)
            {
                ReportError("No se pudo iniciar la captura: " + ex.Message);
                return;
            }

            try
            {
                enroller = new DPFP.Processing.Enrollment();
                totalNeeded = (int)enroller.FeaturesNeeded;
                Console.Error.WriteLine("DEBUG:totalNeeded=" + totalNeeded);
            }
            catch (Exception ex)
            {
                ReportError("No se pudo crear el enrolador: " + ex.Message);
                return;
            }

            Console.Error.WriteLine("STATUS:Coloque el dedo en el lector");

            try
            {
                capturer.StartCapture();
            }
            catch (Exception ex)
            {
                ReportError("Error al iniciar captura: " + ex.Message);
            }
        }

        public void OnComplete(object Capture, string ReaderSerialNumber, DPFP.Sample Sample)
        {
            try
            {
                var extractor = new DPFP.Processing.FeatureExtraction();
                var feedback = DPFP.Capture.CaptureFeedback.None;
                var features = new DPFP.FeatureSet();
                extractor.CreateFeatureSet(Sample, DPFP.Processing.DataPurpose.Enrollment, ref feedback, ref features);

                if (feedback == DPFP.Capture.CaptureFeedback.Good)
                {
                    int neededAntes = (int)enroller.FeaturesNeeded;
                    enroller.AddFeatures(features);
                    int neededDespues = (int)enroller.FeaturesNeeded;
                    var status = enroller.TemplateStatus;

                    int capturadas = totalNeeded - neededDespues;
                    Console.Error.WriteLine("DEBUG:neededAntes=" + neededAntes + " neededDespues=" + neededDespues + " status=" + status);

                    if (status == DPFP.Processing.Enrollment.Status.Failed)
                    {
                        Console.Error.WriteLine("DEBUG:Enrollment failed, clearing and restarting");
                        enroller.Clear();
                        Console.Error.WriteLine("STATUS:Error en enrolamiento, intente de nuevo");
                        return;
                    }

                    Console.Error.WriteLine("PROGRESS:" + capturadas + "/" + totalNeeded);

                    if (neededAntes == neededDespues)
                    {
                        Console.Error.WriteLine("SAMPLE_REJECTED:Muestra repetida o invalida");
                        Console.Error.WriteLine("STATUS:Intente de nuevo");
                    }
                    else
                    {
                        Console.Error.WriteLine("STATUS:Muestra " + capturadas + "/" + totalNeeded + " capturada");

                        if (status == DPFP.Processing.Enrollment.Status.Ready)
                        {
                            DPFP.Template template = enroller.Template;
                            if (template != null)
                            {
                                using (var ms = new MemoryStream())
                                {
                                    template.Serialize(ms);
                                    byte[] bytes = ms.ToArray();
                                    string base64 = Convert.ToBase64String(bytes);
                                    Console.Out.WriteLine(base64);
                                    Console.Out.Flush();
                                }
                                capturer.StopCapture();
                                this.Close();
                            }
                            else
                            {
                                ReportError("Error al generar el template");
                            }
                        }
                    }
                }
                else
                {
                    Console.Error.WriteLine("SAMPLE_REJECTED:Calidad de huella insuficiente");
                    Console.Error.WriteLine("STATUS:Intente de nuevo");
                }
            }
            catch (Exception ex)
            {
                ReportError("Error procesando muestra: " + ex.Message);
            }
        }

        public void OnFingerGone(object Capture, string ReaderSerialNumber)
        {
            Console.Error.WriteLine("STATUS:Dedo retirado, espere...");
        }

        public void OnFingerTouch(object Capture, string ReaderSerialNumber)
        {
            Console.Error.WriteLine("STATUS:Procesando huella...");
        }

        public void OnReaderConnect(object Capture, string ReaderSerialNumber)
        {
            Console.Error.WriteLine("STATUS:Lector conectado");
        }

        public void OnReaderDisconnect(object Capture, string ReaderSerialNumber)
        {
            ReportError("Lector desconectado");
        }

        public void OnSampleQuality(object Capture, string ReaderSerialNumber, DPFP.Capture.CaptureFeedback CaptureFeedback)
        {
        }

        private void ReportError(string msg)
        {
            Console.Error.WriteLine("ERROR:" + msg);
            Console.Error.Flush();
            if (capturer != null)
            {
                try { capturer.StopCapture(); } catch { }
            }
            this.Close();
        }

        protected override void OnFormClosed(FormClosedEventArgs e)
        {
            if (capturer != null)
            {
                try { capturer.StopCapture(); } catch { }
            }
            base.OnFormClosed(e);
        }
    }
}
