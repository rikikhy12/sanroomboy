<?php
defined('BASEPATH') or exit('No direct script access allowed');

class Roomchar extends CI_Controller
{
	protected $role_akses;
	var $menus		= 'pages/menu';
	var $menupopup	= 'pages/menupopup';
	var $tables	= 'tbl_room';

	function __construct()
	{
		parent::__construct();
		$url = base_url();
		if ($this->session->userdata('login') != TRUE) {
			redirect($url, 'refresh');
		}
		$this->load->library('form_validation');
		$this->load->model('M_uniknumber', '_uniknumber');
		$this->load->model('Menu_model', 'menu');
		$this->load->model('master/M_room', 'modeltable');
		$this->load->model('transaksi/M_booking', 'modelbooking');
		$this->load->library('session');
		$this->load->helper('url', 'file');
		$this->load->helper('download');
		$this->load->library('zend');
		$this->role_akses = require_permission();
	}

	public function index()
	{
		$data['judultitle']		= 'ROOM CHAR';
		$file  		= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission = "0";
		$cek		= 	 	$this->role_akses;
		$cekroom		= $this->role_akses;
		if ($cek) {
			$permission = 1;
		}
		if ($permission > 0) {
			$data['content']	= $file . '/' . $akses . '/list_v';
			$data['module']		= 'Room';
			$data['title']		= 'Char';
			$data['detail']		= 'Roomchar  ';
			$data['link']		= $this->uri->segment('1') . "/" . $akses;
			$data['role_akses']	= $cek;
			$data['role_aksesroom']	= $cekroom;
			$data['result']			= $this->modeltable->getdatamodel()->result();
			$data['listtamu']		= $this->modeltable->getdatatamu()->result();
			$data['checkin']	    = date('Y-m-d');
			$data['room']			= $this->modeltable->getdataroom();
			$data['link']			= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	public function listv()
	{
		$data['judultitle']		= 'ROOM CHAR';
		$file  		= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission = "0";
		$cek		= $this->role_akses;
		$cekroom		= $this->role_akses;
		if ($cek) {
			$permission = 1;
		}
		if ($permission > 0) {
			$data['content']	= $file . '/' . $akses . '/list_2';
			$data['module']		= 'Room';
			$data['title']		= 'Char';
			$data['detail']		= 'Roomchar  ';
			$data['link']		= $this->uri->segment('1') . "/" . $akses;
			$data['role_akses']	= $cek;
			$data['role_aksesroom']	= $cekroom;
			$data['result']			= $this->modeltable->getdatamodel()->result();
			$data['listtamu']		= $this->modeltable->getdatatamu()->result();
			$data['checkin']	    = date('Y-m-d');
			$data['room']			= $this->modeltable->getdataroom();
			$data['link']			= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	public function listv3()
	{
		//print_r("aaaaaa"); die;
		$data['judultitle']		= 'ROOM CHAR';
		$file  		= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission = "0";
		$cek		= $this->role_akses;
		$cekroom		= $this->role_akses;
		if ($cek) {
			$permission = 1;
		}
		if ($permission > 0) {
			$data['content']	= $file . '/' . $akses . '/list_3';
			$data['module']		= 'Room';
			$data['title']		= 'Char';
			$data['detail']		= 'Roomchar  ';
			$data['link']		= $this->uri->segment('1') . "/" . $akses;
			$data['role_akses']	= $cek;
			$data['role_aksesroom']	= $cekroom;
			$data['result']			= $this->modeltable->getdatamodel($this->session->userdata('lokasi'))->result();
			$data['listtamu']		= $this->modeltable->getdatatamu($this->session->userdata('lokasi'))->result();
			$data['checkin']	    = date('Y-m-d');
			$data['room']			= $this->modeltable->getdataroom('1');
			$data['link']			= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	public function roomcharsimple()
	{
		//print_r("aaaaaa"); die;
		$data['judultitle']		= 'ROOM CHAR';
		$file  		= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission = "0";
		$cek		= $this->role_akses;
		$cekroom		= $this->role_akses;
		if ($cek) {
			$permission = 1;
		}
		if ($permission > 0) {
			$data['content']	= $file . '/' . $akses . '/list_simple';
			$data['module']		= 'Room';
			$data['title']		= 'Char';
			$data['detail']		= 'Roomchar  ';
			$data['link']		= $this->uri->segment('1') . "/" . $akses;
			$data['role_akses']	= $cek;
			$data['role_aksesroom']	= $cekroom;
			$data['result']			= $this->modeltable->getdatamodel($this->session->userdata('lokasi'))->result();
			$data['listtamu']		= $this->modeltable->getdatatamu($this->session->userdata('lokasi'))->result();
			$data['checkin']	    = date('Y-m-d');
			$data['room']			= $this->modeltable->getdataroom('1');
			$data['link']			= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	public function listpgv()
	{
		$data['judultitle']		= 'ROOM CHAR';
		$file  		= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission = "0";
		$cek		= $this->role_akses;
		$cekroom		= $this->role_akses;
		if ($cek) {
			$permission = 1;
		}
		if ($permission > 0) {
			$data['content']	= $file . '/' . $akses . '/listpgv';
			$data['module']		= 'Room';
			$data['title']		= 'Char';
			$data['detail']		= 'Roomchar  ';
			$data['link']		= $this->uri->segment('1') . "/" . $akses;
			$data['role_akses']	= $cek;
			$data['role_aksesroom']	= $cekroom;
			$data['result']			= $this->modeltable->getdatamodel($this->session->userdata('lokasi'))->result();
			$data['listtamu']		= $this->modeltable->getdatatamu($this->session->userdata('lokasi'))->result();
			$data['checkin']	    = date('Y-m-d');
			$data['room']			= $this->modeltable->getdataroom('2');
			$data['link']			= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	public function listeki()
	{
		$data['judultitle']		= 'ROOM CHAR';
		$file  		= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission = "0";
		$cek		= $this->role_akses;
		$cekroom		= $this->role_akses;
		if ($cek) {
			$permission = 1;
		}
		if ($permission > 0) {
			$data['content']	= $file . '/' . $akses . '/listeki';
			$data['module']		= 'Room';
			$data['title']		= 'Char';
			$data['detail']		= 'Roomchar  ';
			$data['link']		= $this->uri->segment('1') . "/" . $akses;
			$data['role_akses']	= $cek;
			$data['role_aksesroom']	= $cekroom;
			$data['result']			= $this->modeltable->getdatamodel($this->session->userdata('lokasi'))->result();
			$data['listtamu']		= $this->modeltable->getdatatamu($this->session->userdata('lokasi'))->result();
			$data['checkin']	    = date('Y-m-d');
			$data['room']			= $this->modeltable->getdataroom('2');
			$data['link']			= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	function getDataRoom()
	{
		$lokasi = $this->input->get('lokasi');

		$dataroom = $this->db->select('*')->from('tbl_room')->where('lokasi', $lokasi)->where('status_room', '0')->order_by('nomor_room')->get();
		$checkin  = date('Y-m-d');
		foreach ($dataroom->result() as $val):
			$cekbookinginfo         = $this->modelbooking->getinfobooking(substr($checkin, 0, 10), $val->nomor_room);
			$cekcheckininfo         = $this->modelbooking->getinfocheckin(substr($checkin, 0, 10), $val->nomor_room);
			$ceckleaning            = $this->modelbooking->getinfocleaning(substr($checkin, 0, 10), $val->nomor_room);
			$getinfomaintenance     = $this->modelbooking->getinfomaintenance(substr($checkin, 0, 10), $val->nomor_room);
			$ceckcheckout           = $this->modelbooking->getinfocheckout(substr($checkin, 0, 10), $val->nomor_room);
			//	$cekfasilitas           = $this->modeltable->getassetfaslilitasbyno($val->nomor_room);
			//	$tiperoom               = $this->modeltable->gettiperoombynomor($val->nomor_room)->row();
			$pesan                  = $this->modeltable->getpesan($val->nomor_room)->num_rows();
			$getroomKotor           = $this->modelbooking->getroomKotor($val->nomor_room)->result();
			$inforoomboyd 			= $this->modelbooking->getroomboytrakhir($val->nomor_room)->result();
			$data[]		= array(
				'nomor_room'			=> $val->nomor_room,
				'sopa'					=> $val->sopa,
				'kategoriroom'			=> $val->kategori_room,
				'bookinginfo'			=> $cekbookinginfo->result(),
				'checkininfo'			=> $cekcheckininfo->result(),
				'ceckleaning'			=> $ceckleaning->result(),
				'getinfomaintenance'	=> $getinfomaintenance->result(),
				'ceckcheckout'			=> $ceckcheckout->result(),
				//'cekfasilitas'			=>$cekfasilitas->result(),
				//	'tiperoom'				=>$tiperoom,
				'pesan'					=> $pesan,
				'getroomKotor'			=> $getroomKotor,
				'inforoomboyd'			=> $inforoomboyd

			);

		endforeach;
		$jsond     = array(
			'status'	=> true,
			'data'		=> $data,
			'token'		=> $this->security->get_csrf_hash()
		);
		$this->output->set_content_type('application/json')->set_output(json_encode($jsond));
	}
	function getinfobooking()
	{
		$norom = $this->input->get('nomorroom');
		$ci = $this->input->get('checkin');
		$data = $this->modelbooking->getinfobooking($ci, $norom);

		$jsondata = array('data' => $data->row(), 'token' => $this->security->get_csrf_hash());
		$this->output->set_content_type('application/json')->set_output(json_encode($jsondata));
	}

	public function listroom()
	{
		$file  		= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission = "0";
		$cek		= $this->role_akses;
		$cekroom		= $this->role_akses;
		if ($cek) {
			$permission = 1;
		}
		if ($permission > 0) {
			$data['content']	= $file . '/' . $akses . '/list_v';
			$data['module']		= 'Room';
			$data['title']		= 'Char';
			$data['detail']		= 'Roomchar  ';
			$data['link']		= $this->uri->segment('1') . "/" . $akses;
			$data['role_akses']	= $cek;
			$data['role_aksesroom']	= $cekroom;
			$data['result']			= $this->modeltable->getdatamodel()->result();
			$data['listtamu']		= $this->modeltable->getdatatamu()->result();
			$data['checkin']	    = date('Y-m-d');
			if ($this->uri->segment('4') != "") {
				$data['checkin']	    = substr($this->uri->segment('4'), 0, 10);
			}
			$data['room']			= $this->modeltable->getdataroom();
			$data['link']			= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	public function listroom2()
	{
		$file  		= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission = "0";
		$cek		= $this->role_akses;
		$cekroom		= $this->role_akses;
		if ($cek) {
			$permission = 1;
		}
		if ($permission > 0) {
			$data['content']	= $file . '/' . $akses . '/list_2';
			$data['module']		= 'Room';
			$data['title']		= 'Char';
			$data['judultitle']		= 'ROOM Char';
			$data['detail']		= 'Roomchar  ';
			$data['link']		= $this->uri->segment('1') . "/" . $akses;
			$data['role_akses']	= $cek;
			$data['role_aksesroom']	= $cekroom;
			$data['result']			= $this->modeltable->getdatamodel()->result();
			$data['listtamu']		= $this->modeltable->getdatatamu()->result();
			$data['checkin']	    = date('Y-m-d');
			if ($this->uri->segment('4') != "") {
				$data['checkin']	    = substr($this->uri->segment('4'), 0, 10);
			}
			$data['room']			= $this->modeltable->getdataroom();
			$data['link']			= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	function add()
	{
		$data['judultitle']		= 'ROOM CHAR';
		$permission = "0";
		$akses  	= $this->uri->segment('2');
		$cek		= $this->role_akses;
		if ($cek) {
			if ($cek->add) {
				$permission = 1;
			}
		}


		if ($permission > 0) {
			$data['content']		= $this->uri->segment('1') . "/" . $akses . '/add';
			$data['module']			= 'Master';
			$data['title']			= 'Room';
			$data['detail']			= 'Room';
			$data['typeroom']		= $this->modeltable->gettyperoom();

			$data['role_akses']	= $cek;
			$data['link']		= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	function addasset()
	{
		$permission = "0";
		$akses  	= $this->uri->segment('2');
		$cek		= $this->role_akses;
		if ($cek) {
			if ($cek->add) {
				$permission = 1;
			}
		}


		if ($permission > 0) {
			$data['content']		= $this->uri->segment('1') . "/" . $akses . '/addasset';
			$data['module']			= 'Assets';
			$data['title']			= 'Tambah Room Asset';
			$data['detail']			= 'Room';
			$data['dataroom']		= $this->modeltable->getdatamodel();
			$data['detailroomaset']	= $this->modeltable->getdataassetdetail($this->uri->segment('4'), $this->uri->segment('5'));
			$data['dataasset']		= $this->modeltable->getdataassets();
			$data['type']			= $this->uri->segment('4');
			$data['identitas']		= $this->uri->segment('5');

			$data['role_akses']		= $cek;
			$data['link']			= $this->uri->segment('1') . "/" . $akses;

			$this->load->view($this->menupopup, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	function create()
	{
		$permission = "0";
		$akses  	= $this->uri->segment('2');
		$cek		= $this->role_akses;
		if ($cek->add):
			$permission = 1;
		endif;

		if ($permission > 0) {
			$this->form_validation->set_rules('noroom', 'noroom', 'required');
			$this->form_validation->set_rules('typeroom', 'typeroom', 'required');

			if ($this->form_validation->run() == TRUE) {

				$noroom				= $this->input->post('noroom');
				$type				= $this->input->post('typeroom');
				$roomstatus			= $this->input->post('roomstatus');
				$statusmilik		= $this->input->post('statusmilik');
				$namapemilik		= $this->input->post('namapemilik');
				$nohp				= $this->input->post('nohp');
				$nmbank				= $this->input->post('nmbank');
				$norekening			= $this->input->post('norekening');
				$jangkawaktu		= $this->input->post('jangkawaktu');
				$tanggaltempo		= $this->input->post('tanggaltempo');
				$tanggaltempo		= $this->input->post('tanggaltempo');
				$maxroom			= $this->input->post('maxroom');
				$maxroomanak		= $this->input->post('maxroomanak');

				$kode				= $this->_uniknumber->generateKode(
					'R',
					$this->input->post('noroom'),
					'tbl_room',
					'kode_room',
					'create_date',
					date('Y-m')
				);

				$barcode = $this->createbarcode($kode);
				$data		= array(
					'kode_room'					=>	$kode,
					'barcode'					=>	$kode . ".png",
					'nomor_room'				=>	$noroom,
					'jenis_room'				=>	$type,
					'room_status'				=>	$roomstatus,
					'status_pemilik '			=>	$statusmilik,
					'nama_pemilik '				=>	$namapemilik,
					'nama_bank '				=>	$nmbank,
					'no_rekening '				=>	$norekening,
					'no_hp_pemilik '			=>	$nohp,
					'tgl_jatuh_tempo '			=>	$tanggaltempo,
					'jangka_waktu_pembayaran'	=>	$jangkawaktu,
					'maksimal_room'				=>	$maxroom,
					'maksimal_room_anak'		=>	$maxroomanak,
					'create_add'				=>  $this->session->userdata('levelid')
				);
				$activity		= "Menambah data $akses  Kode $akses  $noroom";
				$this->modeltable->_insert("$this->tables", $data);
				$this->_simpanhistory($activity);
				$this->session->set_flashdata('mesagge', '<div class="callout callout-info">
							<h4>Succcess !</h4>
							<p>Data Berhasil Disimpan</p>
						</div>');
				redirect(base_url($this->uri->segment('1') . "/" . $this->uri->segment('2')) . '/add', 'refresh');
			} else {
				$this->session->set_flashdata('mesagge', '<div class="callout callout-danger">
				<h4>Warning <span class="fa fa-times text-right"></span></h4>
				<p> Data gagal Disimpan</p>
			  </div>');
				redirect(base_url($this->uri->segment('1') . "/" . $this->uri->segment('2')) . '/add', 'refresh');
			}
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}
	function createroomasets()
	{
		$permission = "0";
		$akses  	= $this->uri->segment('2');
		$cek		= $this->role_akses;
		if ($cek->add):
			$permission = 1;
		endif;

		if ($permission > 0) {
			$this->form_validation->set_rules('noroom', 'noroom', 'required');
			$this->form_validation->set_rules('assetsid', 'assetsid', 'required');
			$this->form_validation->set_rules('nmmerk', 'nmmerk', 'required');
			$this->form_validation->set_rules('totalassets', 'totalassets', 'required');
			$this->form_validation->set_rules('hargaperassets', 'hargaperassets', 'required');
			$this->form_validation->set_rules('tglpengadaan', 'tglpengadaan', 'required');

			if ($this->form_validation->run() == TRUE) {

				$noroom				= $this->input->post('noroom');
				$assetsid			= $this->input->post('assetsid');
				$nmmerk				= $this->input->post('nmmerk');
				$totalassets		= $this->input->post('totalassets');
				$hargaperassets		= $this->input->post('hargaperassets');
				$tglpengadaan		= $this->input->post('tglpengadaan');


				/*	$kode				=$this->_uniknumber->generateKode('R',$this->input->post('noroom'),
				'tbl_room','kode_room','create_date',date('Y-m'));*/

				/*$barcode =$this->createbarcode($kode);*/
				$data		= array(
					'room_id'					=>  $noroom,
					'asset_id'					=>  $assetsid,
					'merk'						=>  $nmmerk,
					'jumlah'					=>  $totalassets,
					'harga_assets'				=>  $hargaperassets,
					'tgl_asset'					=>  $tglpengadaan,
					'create_add'				=>  $this->session->userdata('levelid')
				);
				$activity		= "Menambah data ROOM ASSET  Kode $akses  $noroom";
				$this->modeltable->_insert("tbl_room_asset", $data);
				$this->_simpanhistory($activity);
				$this->session->set_flashdata('mesagge', '<div class="callout callout-info">
							<h4>Succcess !</h4>
							<p>Data Berhasil Disimpan</p>
						</div>');
				redirect(base_url($this->uri->segment('1') . "/" . $this->uri->segment('2')) . '/addasset/noroom/' . $noroom . '', 'refresh');
			} else {
				$this->session->set_flashdata('mesagge', '<div class="callout callout-danger">
				<h4>Warning <span class="fa fa-times text-right"></span></h4>
				<p> Data gagal Disimpan</p>
			  </div>');
				redirect(base_url($this->uri->segment('1') . "/" . $this->uri->segment('2')) . 'addasset/noroom/' . $noroom . '', 'refresh');
			}
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}

	function createbarcode($kodebarcode)
	{
		$this->load->library('ciqrcode'); //pemanggilan library QR CODE

		$config['cacheable']    = true; //boolean, the default is true
		$config['cachedir']     = './assets/'; //string, the default is application/cache/
		$config['errorlog']     = './assets/'; //string, the default is application/logs/
		$config['imagedir']     = './assets/img/qrcode/'; //direktori penyimpanan qr code
		$config['quality']      = true; //boolean, the default is true
		$config['size']         = '1024'; //interger, the default is 1024
		$config['black']        = array(224, 255, 255); // array, default is array(255,255,255)
		$config['white']        = array(70, 130, 180); // array, default is array(0,0,0)
		$this->ciqrcode->initialize($config);

		$image_name = $kodebarcode . '.png'; //buat name dari qr code sesuai dengan nim

		$params['data'] = $kodebarcode; //data yang akan di jadikan QR CODE
		$params['level'] = 'H'; //H=High
		$params['size'] = 10;
		$params['savename'] = FCPATH . $config['imagedir'] . $image_name; //simpan image QR CODE ke folder assets/images/

		$this->ciqrcode->generate($params);
	}
	function deletebykode()
	{
		$permission = "0";
		$akses  	= $this->uri->segment('2');
		$cek		= $this->role_akses;
		if ($cek->delete):
			$permission = 1;
		endif;

		//cek permision
		if ($permission > 0) {
			$this->form_validation->set_rules('id', 'id', 'required');
			$this->form_validation->set_rules('kodep', 'kodep', 'required');
			$this->form_validation->set_rules('nama', 'nama', 'required');
			if ($this->form_validation->run() == TRUE) {
				$data		= array('id' => $this->input->post('id'));
				$activity	= "menghapus data  room Id " . $this->input->post('id') . " Nama Jenis Room " . $this->input->post('nama');
				$this->modeltable->_hapus("$this->tables", $data);

				$this->_simpanhistory($activity);
				$jsondata	= array(
					'status'	=> true,
					'message'	=> 'Data berhasil di hapus',
				);
			} else {
				$jsondata	= array(
					'status'	=> false,
					'message'	=> 'Data gagal di hapus',
					'kodep'		=> $this->input->post('kodep')
				);
			}
		} else {
			$jsondata	= array(
				'status'	=> false,
				'message'	=> 'Tidak ada hak akses untuk menghapus data',
				'kodep'		=> $this->input->post('kodep')
			);
		}

		$this->output
			->set_content_type('application/json', 'utf-8')
			->set_output(json_encode($jsondata))
			->_display();
		exit;
	}

	function edit()
	{
		$data['judultitle']		= 'ROOM CHAR';
		$module  	= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission = "0";
		$cek		= $this->role_akses;
		if ($cek->edit == 'Y'):
			$permission = 1;
		endif;

		if ($permission > 0) {
			$data['content']	= $module . '/' . $akses . '/edit_v';
			$data['module']		= 'Master';
			$data['title']		= 'Room';
			$data['detail']		= 'Room';
			$data['link']		= $module . '/' . $akses;
			$data['role_akses']	= $cek;
			$data['keyc']		= $this->modeltable->gettablebyid($this->uri->segment('4'));
			$data['typeroom']	= $this->modeltable->gettyperoom();


			$this->load->view($this->menus, $data);
		} else {
			redirect('pages/error_404', 'refresh');
		}
	}

	function saveupdate()
	{

		$module  	= $this->uri->segment('1');
		$akses  	= $this->uri->segment('2');
		$permission	= 0;
		$cek		= $this->role_akses;
		if ($cek->edit == 'Y'): $permission = 1;
		endif;
		if ($permission > 0) {
			$this->form_validation->set_rules('noroom', 'noroom', 'required');
			$this->form_validation->set_rules('typeroom', 'typeroom', 'required');
			$this->form_validation->set_rules('roomstatus', 'roomstatus', 'required');
			$this->form_validation->set_rules('maxroom', 'maxroom', 'required');
			$this->form_validation->set_rules('maxroomanak', 'maxroomanak', 'required');
			$this->form_validation->set_rules('statusmilik', 'statusmilik', 'required');


			$this->form_validation->set_rules('id', 'id', 'required');

			if ($this->form_validation->run() == TRUE) {


				$activity	= "Merubah data Jenis Room, Assets id " . $this->input->post('id') . " Nama Room " . $this->input->post('nama');

				$data		= array(

					'nomor_room'						=> $this->input->post('noroom'),
					'room_status'						=> $this->input->post('roomstatus'),
					'status_pemilik'					=> $this->input->post('statusmilik'),
					'nama_pemilik'						=> $this->input->post('namapemilik'),
					'no_hp_pemilik'						=> $this->input->post('nohp'),
					'no_rekening'						=> $this->input->post('norekening'),
					'nama_bank'							=> $this->input->post('nmbank'),
					'jangka_waktu_pembayaran'			=> $this->input->post('jangkawaktu'),
					'tgl_jatuh_tempo'					=> $this->input->post('tanggaltempo'),
					'jenis_room'						=> $this->input->post('typeroom'),
					'maksimal_room'						=> $this->input->post('maxroom'),
					'maksimal_room_anak'				=> $this->input->post('maxroomanak'),


				);
				$where		= array('id'		=> $this->input->post('id'));
				$this->modeltable->_updates($data, $where);
				$this->_simpanhistory($activity);
				$jsondata	= array('status'	=> true, 'message' => 'Data berhasil di update ');
			} else {
				$jsondata	= array('status'	=> false, 'message' => 'Data gagal di update,Terdapat nilai yang kosong ');
			}
		} else {
			$jsondata	= array(
				'status'	=> false,
				'message'	=> 'Data gagal di update , maaf anda tidak anda akses merubah data departement'
			);
		}

		$this->output
			->set_content_type('application/json', 'utf-8')
			->set_output(json_encode($jsondata))
			->_display();
		exit;
	}

	function _simpanhistory($activity)
	{
		$ip		= $_SERVER['REMOTE_ADDR'];
		$data	= array(
			'employees_nik'		=> $this->session->userdata('nik'),
			'ip_address'		=> $ip,
			'create_date'		=> date('Y-m-d h:i:s'),
			'activity_history'	=> $activity
		);
		$this->modeltable->_insert('employees_activity', $data);
	}
	function deleteassetsrooom()
	{
		$permission = "0";
		$akses  	= $this->uri->segment('2');
		$cek		= $this->role_akses;
		if ($cek->delete):
			$permission = 1;
		endif;

		//cek permision
		if ($permission > 0) {
			$this->form_validation->set_rules('id', 'id', 'required');

			if ($this->form_validation->run() == TRUE) {
				$data		= array('id' => $this->input->post('id'));
				$activity	= "menghapus data ASSET room Id " . $this->input->post('id');
				$this->modeltable->_hapus("tbl_room_asset", $data);

				$this->_simpanhistory($activity);
				$jsondata	= array(
					'status'	=> true,
					'message'	=> 'Data berhasil di hapus',
				);
			} else {
				$jsondata	= array(
					'status'	=> false,
					'message'	=> 'Data gagal di hapus',

				);
			}
		} else {
			$jsondata	= array(
				'status'	=> false,
				'message'	=> 'Tidak ada hak akses untuk menghapus data',

			);
		}

		$this->output
			->set_content_type('application/json', 'utf-8')
			->set_output(json_encode($jsondata))
			->_display();
		exit;
	}
}
